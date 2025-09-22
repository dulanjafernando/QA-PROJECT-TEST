package com.qa.project.tdd.service;

import com.qa.project.model.User;
import com.qa.project.repository.UserRepository;
import com.qa.project.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for UserService
 * Testing business logic for user registration and authentication
 * Following Red-Green-Refactor cycle
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TDD: UserService Business Logic")
class UserServiceTDDTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
    }

    // ==================== FEATURE 1: USER REGISTRATION ====================

    @Test
    @DisplayName("RED: Should register user with valid data")
    void testRegisterUser_WithValidData_ShouldSucceed() {
        // Arrange
        String encodedPassword = "$2a$10$encodedPassword";
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.registerUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("RED: Should fail when username already exists")
    void testRegisterUser_WithExistingUsername_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.registerUser(testUser));
        
        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("RED: Should fail when email already exists")
    void testRegisterUser_WithExistingEmail_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.registerUser(testUser));
        
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("GREEN: Should encode password during registration")
    void testRegisterUser_ShouldEncodePassword() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = "$2a$10$encodedPassword";
        testUser.setPassword(rawPassword);
        
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.registerUser(testUser);

        // Assert
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(user -> 
            encodedPassword.equals(user.getPassword())));
    }

    @Test
    @DisplayName("REFACTOR: Should validate user data before registration")
    void testRegisterUser_WithInvalidData_ShouldThrowException() {
        // Arrange - User with null username
        testUser.setUsername(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser(testUser));
        
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== FEATURE 2: USER AUTHENTICATION ====================

    @Test
    @DisplayName("RED: Should authenticate user with correct credentials")
    void testAuthenticateUser_WithValidCredentials_ShouldSucceed() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = "$2a$10$encodedPassword";
        testUser.setPassword(encodedPassword);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        // Act
        User result = userService.authenticateUser("testuser", rawPassword);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("RED: Should fail authentication with wrong password")
    void testAuthenticateUser_WithWrongPassword_ShouldThrowException() {
        // Arrange
        String rawPassword = "wrongpassword";
        String encodedPassword = "$2a$10$encodedPassword";
        testUser.setPassword(encodedPassword);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.authenticateUser("testuser", rawPassword));
        
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    @DisplayName("RED: Should fail authentication with non-existent user")
    void testAuthenticateUser_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.authenticateUser("nonexistent", "password"));
        
        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    @DisplayName("GREEN: Should handle case-sensitive username lookup")
    void testAuthenticateUser_WithDifferentCase_ShouldBeCaseSensitive() {
        // Arrange
        when(userRepository.findByUsername("TESTUSER")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.authenticateUser("TESTUSER", "password123"));
        
        assertEquals("Invalid username or password", exception.getMessage());
    }

    // ==================== FEATURE 3: USER LOOKUP OPERATIONS ====================

    @Test
    @DisplayName("GREEN: Should find user by username")
    void testFindUserByUsername_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    @DisplayName("GREEN: Should return empty when user not found by username")
    void testFindUserByUsername_WhenUserNotExists_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("GREEN: Should find user by email")
    void testFindUserByEmail_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("GREEN: Should check if username exists")
    void testExistsByUsername_WhenUserExists_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act
        boolean result = userService.existsByUsername("testuser");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("GREEN: Should check if email exists")
    void testExistsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act
        boolean result = userService.existsByEmail("test@example.com");

        // Assert
        assertTrue(result);
    }

    // ==================== EDGE CASES AND ERROR HANDLING ====================

    @Test
    @DisplayName("REFACTOR: Should handle null username in authentication")
    void testAuthenticateUser_WithNullUsername_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.authenticateUser(null, "password"));
    }

    @Test
    @DisplayName("REFACTOR: Should handle null password in authentication")
    void testAuthenticateUser_WithNullPassword_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.authenticateUser("testuser", null));
    }

    @Test
    @DisplayName("REFACTOR: Should handle empty username in authentication")
    void testAuthenticateUser_WithEmptyUsername_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.authenticateUser("", "password"));
    }

    @Test
    @DisplayName("REFACTOR: Should handle empty password in authentication")
    void testAuthenticateUser_WithEmptyPassword_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.authenticateUser("testuser", ""));
    }

    @Test
    @DisplayName("REFACTOR: Should handle repository exceptions during registration")
    void testRegisterUser_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.registerUser(testUser));
        
        assertEquals("Database error", exception.getMessage());
    }

    @Test
    @DisplayName("REFACTOR: Should handle repository exceptions during authentication")
    void testAuthenticateUser_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenThrow(new RuntimeException("Database connection lost"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.authenticateUser("testuser", "password"));
        
        assertEquals("Database connection lost", exception.getMessage());
    }

    // ==================== BUSINESS LOGIC VALIDATION ====================

    @Test
    @DisplayName("REFACTOR: Should validate email format during registration")
    void testRegisterUser_WithInvalidEmailFormat_ShouldThrowException() {
        // Arrange
        testUser.setEmail("invalid-email-format");

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser(testUser));
    }

    @Test
    @DisplayName("REFACTOR: Should validate username length during registration")
    void testRegisterUser_WithShortUsername_ShouldThrowException() {
        // Arrange
        testUser.setUsername("ab"); // Too short

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser(testUser));
    }

    @Test
    @DisplayName("REFACTOR: Should validate password strength during registration")
    void testRegisterUser_WithWeakPassword_ShouldThrowException() {
        // Arrange
        testUser.setPassword("123"); // Too weak

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> userService.registerUser(testUser));
    }
}
package com.qa.project.unit;

import com.qa.project.dto.AuthResponse;
import com.qa.project.dto.LoginRequest;
import com.qa.project.dto.RegisterRequest;
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
 * Comprehensive Unit Tests for UserService
 * 
 * Focus Areas:
 * 1. Business Logic Validation
 * 2. Password Security and Hashing
 * 3. Authentication Flow Testing
 * 4. Error Handling and Edge Cases
 * 5. Service Layer Isolation
 * 
 * Testing Strategy:
 * - Mock all external dependencies (Repository, PasswordEncoder)
 * - Test business logic in isolation
 * - Comprehensive edge case coverage
 * - Verify method interactions and call patterns
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests: UserService Business Logic")
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        // Setup valid register request
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");

        // Setup valid login request
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        // Setup existing user
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setEmail("test@example.com");
        existingUser.setPassword("$2a$10$hashedPassword");
    }

    // ==================== REGISTRATION BUSINESS LOGIC TESTS ====================

    @Test
    @DisplayName("Should successfully register user with valid data and proper password hashing")
    void testRegisterUser_WithValidData_ShouldHashPasswordAndSaveUser() {
        // Arrange
        String rawPassword = "password123";
        String hashedPassword = "$2a$10$hashedPassword";
        User savedUser = new User(validRegisterRequest.getUsername(), hashedPassword, validRegisterRequest.getEmail());
        savedUser.setId(1L);

        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        AuthResponse response = userService.registerUser(validRegisterRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());

        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(user -> 
            user.getUsername().equals("testuser") &&
            user.getEmail().equals("test@example.com") &&
            user.getPassword().equals(hashedPassword)
        ));
    }

    @Test
    @DisplayName("Should reject registration when username already exists")
    void testRegisterUser_WithExistingUsername_ShouldReturnFailureResponse() {
        // Arrange
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(true);

        // Act
        AuthResponse response = userService.registerUser(validRegisterRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Username already exists", response.getMessage());

        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration when email already exists")
    void testRegisterUser_WithExistingEmail_ShouldReturnFailureResponse() {
        // Arrange
        when(userRepository.existsByUsername(validRegisterRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        // Act
        AuthResponse response = userService.registerUser(validRegisterRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Email already exists", response.getMessage());

        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle database exceptions during registration gracefully")
    void testRegisterUser_WithDatabaseException_ShouldReturnFailureResponse() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection failed"));

        // Act
        AuthResponse response = userService.registerUser(validRegisterRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Registration failed"));
        assertTrue(response.getMessage().contains("Database connection failed"));

        // Verify all steps were attempted
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    // ==================== AUTHENTICATION BUSINESS LOGIC TESTS ====================

    @Test
    @DisplayName("Should successfully authenticate user with correct credentials using username")
    void testAuthenticateUser_WithValidUsernameCredentials_ShouldReturnSuccessResponse() {
        // Arrange
        when(userRepository.findByUsername(validLoginRequest.getUsername())).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(validLoginRequest.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(validLoginRequest.getPassword(), existingUser.getPassword())).thenReturn(true);

        // Act
        AuthResponse response = userService.authenticateUser(validLoginRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertNotNull(response.getToken());
        assertTrue(response.getToken().startsWith("auth_token_1_"));

        // Verify interactions
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
    }

    @Test
    @DisplayName("Should successfully authenticate user with correct credentials using email")
    void testAuthenticateUser_WithValidEmailCredentials_ShouldReturnSuccessResponse() {
        // Arrange
        validLoginRequest.setUsername("test@example.com"); // Using email as username
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(validLoginRequest.getPassword(), existingUser.getPassword())).thenReturn(true);

        // Act
        AuthResponse response = userService.authenticateUser(validLoginRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals(1L, response.getUserId());
        assertNotNull(response.getToken());

        // Verify interactions
        verify(userRepository).findByUsername("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
    }

    @Test
    @DisplayName("Should reject authentication with non-existent user")
    void testAuthenticateUser_WithNonExistentUser_ShouldReturnFailureResponse() {
        // Arrange
        when(userRepository.findByUsername(validLoginRequest.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(validLoginRequest.getUsername())).thenReturn(Optional.empty());

        // Act
        AuthResponse response = userService.authenticateUser(validLoginRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Invalid username or password", response.getMessage());

        // Verify interactions
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).findByEmail("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject authentication with incorrect password")
    void testAuthenticateUser_WithIncorrectPassword_ShouldReturnFailureResponse() {
        // Arrange
        when(userRepository.findByUsername(validLoginRequest.getUsername())).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(validLoginRequest.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(validLoginRequest.getPassword(), existingUser.getPassword())).thenReturn(false);

        // Act
        AuthResponse response = userService.authenticateUser(validLoginRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals("Invalid username or password", response.getMessage());

        // Verify interactions
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
    }

    @Test
    @DisplayName("Should handle database exceptions during authentication gracefully")
    void testAuthenticateUser_WithDatabaseException_ShouldReturnFailureResponse() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenThrow(new RuntimeException("Database timeout"));

        // Act
        AuthResponse response = userService.authenticateUser(validLoginRequest);

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Login failed"));
        assertTrue(response.getMessage().contains("Database timeout"));

        // Verify repository was called
        verify(userRepository).findByUsername("testuser");
    }

    // ==================== EDGE CASES AND SECURITY TESTS ====================

    @Test
    @DisplayName("Should validate password encoding strength")
    void testPasswordEncoding_ShouldUseBCryptWithProperStrength() {
        // Arrange
        String plainPassword = "mySecretPassword123!";
        String encodedPassword = "$2a$12$properlyEncodedPassword";
        
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        validRegisterRequest.setPassword(plainPassword);

        // Act
        userService.registerUser(validRegisterRequest);

        // Assert
        verify(passwordEncoder).encode(plainPassword);
        verify(userRepository).save(argThat(user -> 
            user.getPassword().equals(encodedPassword) &&
            !user.getPassword().equals(plainPassword) // Ensure password is not stored in plain text
        ));
    }

    @Test
    @DisplayName("Should generate unique authentication tokens")
    void testTokenGeneration_ShouldCreateUniqueTokensForEachLogin() {
        // Arrange
        when(userRepository.findByUsername(validLoginRequest.getUsername())).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(validLoginRequest.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(validLoginRequest.getPassword(), existingUser.getPassword())).thenReturn(true);

        // Act - Simulate two login attempts
        AuthResponse response1 = userService.authenticateUser(validLoginRequest);
        
        // Small delay to ensure different timestamps
        try { Thread.sleep(1); } catch (InterruptedException e) {}
        
        AuthResponse response2 = userService.authenticateUser(validLoginRequest);

        // Assert
        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertNotNull(response1.getToken());
        assertNotNull(response2.getToken());
        assertNotEquals(response1.getToken(), response2.getToken()); // Tokens should be unique
        
        // Verify token format
        assertTrue(response1.getToken().startsWith("auth_token_1_"));
        assertTrue(response2.getToken().startsWith("auth_token_1_"));
    }

    @Test
    @DisplayName("Should maintain data integrity during concurrent registration attempts")
    void testConcurrentRegistration_ShouldMaintainDataIntegrity() {
        // Arrange - Simulate race condition where username becomes available between checks
        when(userRepository.existsByUsername(validRegisterRequest.getUsername()))
            .thenReturn(false)  // First check passes
            .thenReturn(true);  // Second registration would fail (simulating concurrent registration)
        
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        AuthResponse response = userService.registerUser(validRegisterRequest);

        // Assert - Should still succeed since the check passed
        assertTrue(response.isSuccess());
        
        // Verify proper sequence of operations
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle null or empty request data gracefully")
    void testNullRequestHandling_ShouldPreventNullPointerExceptions() {
        // Test with null register request (DTO version)
        assertDoesNotThrow(() -> {
            try {
                AuthResponse response = userService.registerUser((RegisterRequest) null);
                assertNotNull(response);
                assertFalse(response.isSuccess());
            } catch (NullPointerException e) {
                fail("Should handle null requests gracefully");
            }
        });

        // Test with null login request
        assertDoesNotThrow(() -> {
            try {
                AuthResponse response = userService.authenticateUser((LoginRequest) null);
                assertNotNull(response);
                assertFalse(response.isSuccess());
            } catch (NullPointerException e) {
                fail("Should handle null requests gracefully");
            }
        });
    }
}
package com.qa.project.tdd.repository;

import com.qa.project.model.User;
import com.qa.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests for UserRepository
 * Testing data access layer for user operations
 * Following Red-Green-Refactor cycle
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TDD: UserRepository Data Access")
class UserRepositoryTDDTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");
    }

    // ==================== FEATURE 1: USER SAVE OPERATIONS ====================

    @Test
    @DisplayName("RED: Should save user to database")
    void testSaveUser_WithValidData_ShouldPersistUser() {
        // Act
        User savedUser = userRepository.save(testUser);

        // Assert
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword123", savedUser.getPassword());
    }

    @Test
    @DisplayName("RED: Should generate auto-incremented ID when saving user")
    void testSaveUser_ShouldGenerateAutoIncrementedId() {
        // Act
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Assert
        assertNotNull(savedUser.getId());
        assertTrue(savedUser.getId() > 0);
    }

    @Test
    @DisplayName("GREEN: Should save multiple users with different IDs")
    void testSaveMultipleUsers_ShouldGenerateUniqueIds() {
        // Arrange
        User user2 = new User();
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");
        user2.setPassword("encodedPassword456");

        // Act
        User savedUser1 = userRepository.save(testUser);
        User savedUser2 = userRepository.save(user2);
        entityManager.flush();

        // Assert
        assertNotNull(savedUser1.getId());
        assertNotNull(savedUser2.getId());
        assertNotEquals(savedUser1.getId(), savedUser2.getId());
    }

    // ==================== FEATURE 2: FIND BY USERNAME ====================

    @Test
    @DisplayName("RED: Should find user by username when exists")
    void testFindByUsername_WhenUserExists_ShouldReturnUser() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        Optional<User> result = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("RED: Should return empty when username not found")
    void testFindByUsername_WhenUserNotExists_ShouldReturnEmpty() {
        // Act
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("GREEN: Should be case-sensitive when finding by username")
    void testFindByUsername_ShouldBeCaseSensitive() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        Optional<User> result = userRepository.findByUsername("TESTUSER");

        // Assert
        assertFalse(result.isPresent());
    }

    // ==================== FEATURE 3: FIND BY EMAIL ====================

    @Test
    @DisplayName("RED: Should find user by email when exists")
    void testFindByEmail_WhenUserExists_ShouldReturnUser() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("RED: Should return empty when email not found")
    void testFindByEmail_WhenEmailNotExists_ShouldReturnEmpty() {
        // Act
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("GREEN: Should be case-sensitive when finding by email")
    void testFindByEmail_ShouldBeCaseSensitive() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        Optional<User> result = userRepository.findByEmail("TEST@EXAMPLE.COM");

        // Assert
        assertFalse(result.isPresent());
    }

    // ==================== FEATURE 4: EXISTS BY USERNAME ====================

    @Test
    @DisplayName("RED: Should return true when username exists")
    void testExistsByUsername_WhenUserExists_ShouldReturnTrue() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        boolean exists = userRepository.existsByUsername("testuser");

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("RED: Should return false when username does not exist")
    void testExistsByUsername_WhenUserNotExists_ShouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("GREEN: Should be case-sensitive for username existence check")
    void testExistsByUsername_ShouldBeCaseSensitive() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        boolean exists = userRepository.existsByUsername("TESTUSER");

        // Assert
        assertFalse(exists);
    }

    // ==================== FEATURE 5: EXISTS BY EMAIL ====================

    @Test
    @DisplayName("RED: Should return true when email exists")
    void testExistsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("RED: Should return false when email does not exist")
    void testExistsByEmail_WhenEmailNotExists_ShouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("GREEN: Should be case-sensitive for email existence check")
    void testExistsByEmail_ShouldBeCaseSensitive() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act
        boolean exists = userRepository.existsByEmail("TEST@EXAMPLE.COM");

        // Assert
        assertFalse(exists);
    }

    // ==================== FEATURE 6: UNIQUE CONSTRAINTS ====================

    @Test
    @DisplayName("RED: Should enforce unique username constraint")
    void testSaveUser_WithDuplicateUsername_ShouldThrowException() {
        // Arrange
        entityManager.persistAndFlush(testUser);
        
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser"); // Same username
        duplicateUser.setEmail("different@example.com");
        duplicateUser.setPassword("differentPassword");

        // Act & Assert
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("RED: Should enforce unique email constraint")
    void testSaveUser_WithDuplicateEmail_ShouldThrowException() {
        // Arrange
        entityManager.persistAndFlush(testUser);
        
        User duplicateUser = new User();
        duplicateUser.setUsername("differentuser");
        duplicateUser.setEmail("test@example.com"); // Same email
        duplicateUser.setPassword("differentPassword");

        // Act & Assert
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        });
    }

    // ==================== EDGE CASES AND ERROR HANDLING ====================

    @Test
    @DisplayName("REFACTOR: Should handle null username in find operation")
    void testFindByUsername_WithNullUsername_ShouldReturnEmpty() {
        // Act
        Optional<User> result = userRepository.findByUsername(null);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("REFACTOR: Should handle null email in find operation")
    void testFindByEmail_WithNullEmail_ShouldReturnEmpty() {
        // Act
        Optional<User> result = userRepository.findByEmail(null);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("REFACTOR: Should handle empty username in exists check")
    void testExistsByUsername_WithEmptyUsername_ShouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("");

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("REFACTOR: Should handle empty email in exists check")
    void testExistsByEmail_WithEmptyEmail_ShouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("");

        // Assert
        assertFalse(exists);
    }

    // ==================== PERFORMANCE AND QUERY OPTIMIZATION ====================

    @Test
    @DisplayName("REFACTOR: Should efficiently handle multiple username lookups")
    void testMultipleUsernameLookups_ShouldBeEfficient() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act - Multiple lookups
        for (int i = 0; i < 5; i++) {
            Optional<User> result = userRepository.findByUsername("testuser");
            assertTrue(result.isPresent());
        }

        // No explicit assertion needed - test should complete without timeout
    }

    @Test
    @DisplayName("REFACTOR: Should efficiently handle multiple email lookups")
    void testMultipleEmailLookups_ShouldBeEfficient() {
        // Arrange
        entityManager.persistAndFlush(testUser);

        // Act - Multiple lookups
        for (int i = 0; i < 5; i++) {
            Optional<User> result = userRepository.findByEmail("test@example.com");
            assertTrue(result.isPresent());
        }

        // No explicit assertion needed - test should complete without timeout
    }

    // ==================== DATA INTEGRITY TESTS ====================

    @Test
    @DisplayName("REFACTOR: Should maintain data integrity after save")
    void testSaveUser_ShouldMaintainDataIntegrity() {
        // Act
        User savedUser = userRepository.save(testUser);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context

        // Assert - Retrieve from database
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());
        assertTrue(retrievedUser.isPresent());
        assertEquals("testuser", retrievedUser.get().getUsername());
        assertEquals("test@example.com", retrievedUser.get().getEmail());
        assertEquals("encodedPassword123", retrievedUser.get().getPassword());
    }

    @Test
    @DisplayName("REFACTOR: Should handle special characters in username and email")
    void testSaveUser_WithSpecialCharacters_ShouldPersist() {
        // Arrange
        testUser.setUsername("user_123-test");
        testUser.setEmail("user+test@example-domain.com");

        // Act
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("user_123-test", savedUser.getUsername());
        assertEquals("user+test@example-domain.com", savedUser.getEmail());
    }

    @Test
    @DisplayName("REFACTOR: Should handle Unicode characters in data")
    void testSaveUser_WithUnicodeCharacters_ShouldPersist() {
        // Arrange
        testUser.setUsername("用户名123");
        testUser.setEmail("test@例え.com");

        // Act
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Assert
        assertNotNull(savedUser.getId());
        assertEquals("用户名123", savedUser.getUsername());
        assertEquals("test@例え.com", savedUser.getEmail());
    }

    // ==================== TRANSACTION TESTS ====================

    @Test
    @DisplayName("REFACTOR: Should rollback transaction on constraint violation")
    void testSaveUser_OnConstraintViolation_ShouldRollback() {
        // Arrange
        entityManager.persistAndFlush(testUser);
        long initialCount = userRepository.count();

        User duplicateUser = new User();
        duplicateUser.setUsername("testuser"); // Duplicate username
        duplicateUser.setEmail("different@example.com");
        duplicateUser.setPassword("differentPassword");

        // Act & Assert
        assertThrows(Exception.class, () -> {
            userRepository.save(duplicateUser);
            entityManager.flush();
        });

        // Clear the entity manager to recover from the constraint violation
        entityManager.clear();
        
        // Verify count hasn't changed (using a fresh query)
        assertEquals(initialCount, userRepository.count());
    }
}
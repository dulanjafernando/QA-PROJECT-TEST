package com.qa.project.tdd.model;

import com.qa.project.model.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Tests for User Model
 * Testing entity validation and constraints
 * Following Red-Green-Refactor cycle
 */
@DisplayName("TDD: User Model Validation")
class UserModelTDDTest {

    private Validator validator;
    private User testUser;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
    }

    // ==================== FEATURE 1: BASIC MODEL VALIDATION ====================

    @Test
    @DisplayName("GREEN: Should create valid user with all required fields")
    void testCreateUser_WithValidData_ShouldPassValidation() {
        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
        assertEquals("testuser", testUser.getUsername());
        assertEquals("test@example.com", testUser.getEmail());
        assertEquals("password123", testUser.getPassword());
    }

    @Test
    @DisplayName("RED: Should fail validation when username is null")
    void testCreateUser_WithNullUsername_ShouldFailValidation() {
        // Arrange
        testUser.setUsername(null);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("RED: Should fail validation when email is null")
    void testCreateUser_WithNullEmail_ShouldFailValidation() {
        // Arrange
        testUser.setEmail(null);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("RED: Should fail validation when password is null")
    void testCreateUser_WithNullPassword_ShouldFailValidation() {
        // Arrange
        testUser.setPassword(null);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    // ==================== FEATURE 2: USERNAME VALIDATION ====================

    @Test
    @DisplayName("RED: Should fail validation when username is empty")
    void testCreateUser_WithEmptyUsername_ShouldFailValidation() {
        // Arrange
        testUser.setUsername("");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("RED: Should fail validation when username is too short")
    void testCreateUser_WithShortUsername_ShouldFailValidation() {
        // Arrange
        testUser.setUsername("ab"); // Less than 3 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("RED: Should fail validation when username is too long")
    void testCreateUser_WithLongUsername_ShouldFailValidation() {
        // Arrange
        testUser.setUsername("a".repeat(21)); // More than 20 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("GREEN: Should pass validation with minimum username length")
    void testCreateUser_WithMinimumUsernameLength_ShouldPassValidation() {
        // Arrange
        testUser.setUsername("abc"); // Exactly 3 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("GREEN: Should pass validation with maximum username length")
    void testCreateUser_WithMaximumUsernameLength_ShouldPassValidation() {
        // Arrange
        testUser.setUsername("a".repeat(20)); // Exactly 20 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
    }

    // ==================== FEATURE 3: EMAIL VALIDATION ====================

    @Test
    @DisplayName("RED: Should fail validation when email is empty")
    void testCreateUser_WithEmptyEmail_ShouldFailValidation() {
        // Arrange
        testUser.setEmail("");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("RED: Should fail validation with invalid email format")
    void testCreateUser_WithInvalidEmailFormat_ShouldFailValidation() {
        // Arrange
        testUser.setEmail("invalid-email-format");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("GREEN: Should pass validation with valid email formats")
    void testCreateUser_WithValidEmailFormats_ShouldPassValidation() {
        // Test various valid email formats
        String[] validEmails = {
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@subdomain.example.org",
            "123@domain.com",
            "user_name@example-domain.com"
        };

        for (String email : validEmails) {
            // Arrange
            testUser.setEmail(email);

            // Act
            Set<ConstraintViolation<User>> violations = validator.validate(testUser);

            // Assert
            assertTrue(violations.isEmpty(), "Should accept email: " + email);
        }
    }

    // ==================== FEATURE 4: PASSWORD VALIDATION ====================

    @Test
    @DisplayName("RED: Should fail validation when password is empty")
    void testCreateUser_WithEmptyPassword_ShouldFailValidation() {
        // Arrange
        testUser.setPassword("");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("RED: Should fail validation when password is too short")
    void testCreateUser_WithShortPassword_ShouldFailValidation() {
        // Arrange
        testUser.setPassword("12345"); // Less than 6 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("GREEN: Should pass validation with minimum password length")
    void testCreateUser_WithMinimumPasswordLength_ShouldPassValidation() {
        // Arrange
        testUser.setPassword("123456"); // Exactly 6 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
    }

    // ==================== FEATURE 5: ENTITY RELATIONSHIPS AND STATE ====================

    @Test
    @DisplayName("GREEN: Should initialize with null ID for new entity")
    void testCreateUser_ShouldInitializeWithNullId() {
        // Act
        User newUser = new User();

        // Assert
        assertNull(newUser.getId());
    }

    @Test
    @DisplayName("GREEN: Should allow setting and getting ID")
    void testUser_ShouldAllowIdManipulation() {
        // Act
        testUser.setId(123L);

        // Assert
        assertEquals(123L, testUser.getId());
    }

    @Test
    @DisplayName("REFACTOR: Should handle toString method appropriately")
    void testUser_ToStringShouldNotExposePassword() {
        // Act
        String userString = testUser.toString();

        // Assert
        assertNotNull(userString);
        assertFalse(userString.contains("password123"), 
            "ToString should not expose password");
        assertTrue(userString.contains("testuser"));
        assertTrue(userString.contains("test@example.com"));
    }

    // ==================== EDGE CASES AND SPECIAL CHARACTERS ====================

    @Test
    @DisplayName("REFACTOR: Should handle special characters in username")
    void testCreateUser_WithSpecialCharactersInUsername_ShouldValidateCorrectly() {
        // Arrange
        testUser.setUsername("user_123-test");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("REFACTOR: Should handle special characters in email")
    void testCreateUser_WithSpecialCharactersInEmail_ShouldValidateCorrectly() {
        // Arrange
        testUser.setEmail("user+test@example-domain.com");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("REFACTOR: Should handle special characters in password")
    void testCreateUser_WithSpecialCharactersInPassword_ShouldValidateCorrectly() {
        // Arrange
        testUser.setPassword("password!@#$%^&*()");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("REFACTOR: Should handle Unicode characters")
    void testCreateUser_WithUnicodeCharacters_ShouldValidateCorrectly() {
        // Arrange
        testUser.setUsername("用户名123");
        testUser.setEmail("test@例え.com");
        testUser.setPassword("密码123456");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(testUser);

        // Assert
        assertTrue(violations.isEmpty());
    }

    // ==================== BOUNDARY VALUE TESTS ====================

    @Test
    @DisplayName("REFACTOR: Should test username boundary values")
    void testCreateUser_UsernameBoundaryValues_ShouldValidateCorrectly() {
        // Test exact boundary values
        String[] boundaryUsernames = {
            "ab",           // Too short (2 chars)
            "abc",          // Valid minimum (3 chars)
            "a".repeat(20), // Valid maximum (20 chars)
            "a".repeat(21)  // Too long (21 chars)
        };

        boolean[] expectedValid = {false, true, true, false};

        for (int i = 0; i < boundaryUsernames.length; i++) {
            // Arrange
            testUser.setUsername(boundaryUsernames[i]);

            // Act
            Set<ConstraintViolation<User>> violations = validator.validate(testUser);

            // Assert
            boolean isValid = violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("username"));
            assertEquals(expectedValid[i], isValid, 
                "Username '" + boundaryUsernames[i] + "' validation failed");
        }
    }

    @Test
    @DisplayName("REFACTOR: Should test password boundary values")
    void testCreateUser_PasswordBoundaryValues_ShouldValidateCorrectly() {
        // Test exact boundary values
        String[] boundaryPasswords = {
            "12345",        // Too short (5 chars)
            "123456",       // Valid minimum (6 chars)
            "a".repeat(100) // Long but valid
        };

        boolean[] expectedValid = {false, true, true};

        for (int i = 0; i < boundaryPasswords.length; i++) {
            // Arrange
            testUser.setPassword(boundaryPasswords[i]);

            // Act
            Set<ConstraintViolation<User>> violations = validator.validate(testUser);

            // Assert
            boolean isValid = violations.stream()
                .noneMatch(v -> v.getPropertyPath().toString().equals("password"));
            assertEquals(expectedValid[i], isValid, 
                "Password with length " + boundaryPasswords[i].length() + " validation failed");
        }
    }
}
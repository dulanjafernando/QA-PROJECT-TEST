package com.qa.project.unit;

import com.qa.project.model.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Tests for User Model
 * 
 * Focus Areas:
 * 1. Bean Validation Constraints
 * 2. Data Integrity Rules
 * 3. Business Rule Enforcement
 * 4. Edge Cases and Boundary Testing
 * 5. Model State Validation
 * 
 * Testing Strategy:
 * - Test all validation annotations
 * - Verify constraint messages
 * - Test boundary conditions
 * - Validate model integrity
 * - Test constructor variations
 */
@DisplayName("Unit Tests: User Model Validation")
class UserModelValidationUnitTest {

    private Validator validator;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        // Setup valid user for testing
        user = new User();
        user.setUsername("validuser");
        user.setEmail("valid@example.com");
        user.setPassword("validpass123");
    }

    // ==================== USERNAME VALIDATION TESTS ====================

    @Test
    @DisplayName("Should validate correct username successfully")
    void testUsernameValidation_WithValidUsername_ShouldPassValidation() {
        // Arrange
        user.setUsername("validuser123");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertTrue(violations.isEmpty(), "Valid username should not produce violations");
    }

    @Test
    @DisplayName("Should reject null username")
    void testUsernameValidation_WithNullUsername_ShouldFailValidation() {
        // Arrange
        user.setUsername(null);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Username is required")));
    }

    @Test
    @DisplayName("Should reject empty username")
    void testUsernameValidation_WithEmptyUsername_ShouldFailValidation() {
        // Arrange
        user.setUsername("");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username") && 
                          v.getMessage().contains("Username is required")));
    }

    @Test
    @DisplayName("Should reject username that is too short")
    void testUsernameValidation_WithShortUsername_ShouldFailValidation() {
        // Arrange
        user.setUsername("ab"); // Only 2 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username") && 
                          v.getMessage().contains("Username must be between 3 and 20 characters")));
    }

    @Test
    @DisplayName("Should reject username that is too long")
    void testUsernameValidation_WithLongUsername_ShouldFailValidation() {
        // Arrange
        user.setUsername("thisUsernameIsWayTooLongForValidation"); // More than 20 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username") && 
                          v.getMessage().contains("Username must be between 3 and 20 characters")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "user", "test123", "validuser", "user_name", "user.name", "user-name", "12345678901234567890"})
    @DisplayName("Should validate usernames within valid length range")
    void testUsernameValidation_WithValidLengthUsernames_ShouldPassValidation(String username) {
        // Arrange
        user.setUsername(username);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        boolean hasUsernameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        assertFalse(hasUsernameViolation, "Username '" + username + "' should be valid");
    }

    // ==================== EMAIL VALIDATION TESTS ====================

    @Test
    @DisplayName("Should validate correct email format successfully")
    void testEmailValidation_WithValidEmail_ShouldPassValidation() {
        // Arrange
        user.setEmail("test@example.com");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertTrue(violations.isEmpty(), "Valid email should not produce violations");
    }

    @Test
    @DisplayName("Should reject null email")
    void testEmailValidation_WithNullEmail_ShouldFailValidation() {
        // Arrange
        user.setEmail(null);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email") && 
                          v.getMessage().contains("Email is required")));
    }

    @Test
    @DisplayName("Should reject empty email")
    void testEmailValidation_WithEmptyEmail_ShouldFailValidation() {
        // Arrange
        user.setEmail("");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email") && 
                          v.getMessage().contains("Email is required")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid.email", "test@", "@example.com", "test..test@example.com", "test@.com"})
    @DisplayName("Should reject invalid email formats")
    void testEmailValidation_WithInvalidEmailFormats_ShouldFailValidation(String email) {
        // Arrange
        user.setEmail(email);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email") && 
                          v.getMessage().contains("Email should be valid"));
        assertTrue(hasEmailViolation, "Email '" + email + "' should be invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "user.name@domain.co.uk", "user+tag@example.org", "123@456.com"})
    @DisplayName("Should validate correct email formats")
    void testEmailValidation_WithValidEmailFormats_ShouldPassValidation(String email) {
        // Arrange
        user.setEmail(email);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        boolean hasEmailViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertFalse(hasEmailViolation, "Email '" + email + "' should be valid");
    }

    // ==================== PASSWORD VALIDATION TESTS ====================

    @Test
    @DisplayName("Should validate correct password successfully")
    void testPasswordValidation_WithValidPassword_ShouldPassValidation() {
        // Arrange
        user.setPassword("validpass123");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertTrue(violations.isEmpty(), "Valid password should not produce violations");
    }

    @Test
    @DisplayName("Should reject null password")
    void testPasswordValidation_WithNullPassword_ShouldFailValidation() {
        // Arrange
        user.setPassword(null);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password") && 
                          v.getMessage().contains("Password is required")));
    }

    @Test
    @DisplayName("Should reject empty password")
    void testPasswordValidation_WithEmptyPassword_ShouldFailValidation() {
        // Arrange
        user.setPassword("");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password") && 
                          v.getMessage().contains("Password is required")));
    }

    @Test
    @DisplayName("Should reject password that is too short")
    void testPasswordValidation_WithShortPassword_ShouldFailValidation() {
        // Arrange
        user.setPassword("12345"); // Only 5 characters

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password") && 
                          v.getMessage().contains("Password must be at least 6 characters")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "password", "mypassword123", "very_long_password_that_should_be_valid"})
    @DisplayName("Should validate passwords with valid length")
    void testPasswordValidation_WithValidLengthPasswords_ShouldPassValidation(String password) {
        // Arrange
        user.setPassword(password);

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        boolean hasPasswordViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertFalse(hasPasswordViolation, "Password '" + password + "' should be valid");
    }

    // ==================== CONSTRUCTOR AND MODEL INTEGRITY TESTS ====================

    @Test
    @DisplayName("Should create user with default constructor")
    void testDefaultConstructor_ShouldCreateEmptyUser() {
        // Act
        User newUser = new User();

        // Assert
        assertNotNull(newUser);
        assertNull(newUser.getId());
        assertNull(newUser.getUsername());
        assertNull(newUser.getEmail());
        assertNull(newUser.getPassword());
    }

    @Test
    @DisplayName("Should create user with parameterized constructor")
    void testParameterizedConstructor_ShouldCreateUserWithProvidedData() {
        // Act
        User newUser = new User("testuser", "password123", "test@example.com");

        // Assert
        assertNotNull(newUser);
        assertEquals("testuser", newUser.getUsername());
        assertEquals("password123", newUser.getPassword());
        assertEquals("test@example.com", newUser.getEmail());
        assertNull(newUser.getId()); // ID should be null until persisted
    }

    @Test
    @DisplayName("Should maintain data integrity through getter/setter operations")
    void testGetterSetterIntegrity_ShouldMaintainDataConsistency() {
        // Arrange
        User testUser = new User();

        // Act
        testUser.setId(100L);
        testUser.setUsername("integrationtest");
        testUser.setEmail("integration@test.com");
        testUser.setPassword("testpassword");

        // Assert
        assertEquals(100L, testUser.getId());
        assertEquals("integrationtest", testUser.getUsername());
        assertEquals("integration@test.com", testUser.getEmail());
        assertEquals("testpassword", testUser.getPassword());
    }

    @Test
    @DisplayName("Should handle all validation errors simultaneously")
    void testCompleteValidation_WithAllInvalidFields_ShouldReturnMultipleViolations() {
        // Arrange
        User invalidUser = new User();
        invalidUser.setUsername("ab"); // Too short
        invalidUser.setEmail("invalid.email"); // Invalid format
        invalidUser.setPassword("123"); // Too short

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(invalidUser);

        // Assert
        assertFalse(violations.isEmpty());
        assertEquals(3, violations.size(), "Should have exactly 3 validation violations");

        // Verify each field has a violation
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Should validate completely valid user without any violations")
    void testCompleteValidation_WithAllValidFields_ShouldPassValidation() {
        // Arrange
        User validUser = new User("validuser", "validpassword123", "valid@example.com");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(validUser);

        // Assert
        assertTrue(violations.isEmpty(), "Completely valid user should have no violations");
    }

    // ==================== BOUNDARY VALUE TESTS ====================

    @Test
    @DisplayName("Should validate username at minimum length boundary")
    void testUsernameBoundary_WithMinimumLength_ShouldPassValidation() {
        // Arrange - Exactly 3 characters (minimum)
        user.setUsername("abc");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        boolean hasUsernameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        assertFalse(hasUsernameViolation, "Username with exactly 3 characters should be valid");
    }

    @Test
    @DisplayName("Should validate username at maximum length boundary")
    void testUsernameBoundary_WithMaximumLength_ShouldPassValidation() {
        // Arrange - Exactly 20 characters (maximum)
        user.setUsername("12345678901234567890");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        boolean hasUsernameViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        assertFalse(hasUsernameViolation, "Username with exactly 20 characters should be valid");
    }

    @Test
    @DisplayName("Should validate password at minimum length boundary")
    void testPasswordBoundary_WithMinimumLength_ShouldPassValidation() {
        // Arrange - Exactly 6 characters (minimum)
        user.setPassword("123456");

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        boolean hasPasswordViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertFalse(hasPasswordViolation, "Password with exactly 6 characters should be valid");
    }

    @Test
    @DisplayName("Should handle whitespace-only values appropriately")
    void testWhitespaceValidation_ShouldTreatAsBlank() {
        // Arrange
        user.setUsername("   "); // Whitespace only
        user.setEmail("   "); // Whitespace only
        user.setPassword("   "); // Whitespace only

        // Act
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }
}
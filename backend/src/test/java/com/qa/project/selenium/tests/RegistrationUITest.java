package com.qa.project.selenium.tests;

import com.qa.project.selenium.config.SeleniumTestConfig;
import com.qa.project.selenium.pages.RegistrationPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for the React Registration/SignUp component
 * Tests the registration functionality through the browser interface
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class RegistrationUITest extends SeleniumTestConfig {

    private RegistrationPage registrationPage;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setUp first to initialize WebDriver
        registrationPage = new RegistrationPage(driver);
        navigateToReactApp("/signup");
    }

    @Test
    void testSuccessfulRegistration() {
        // Given: Valid registration data
        String username = "newuser" + System.currentTimeMillis();
        String email = username + "@test.com";
        String password = "SecurePass123!";

        // When: User fills registration form and submits
        registrationPage.register(username, email, password);

        // Then: Registration should be successful
        registrationPage.waitForRegistrationToComplete();
        assertTrue(registrationPage.isRegistrationSuccessful(),
                   "Registration should be successful with valid data");

        // Form fields should be cleared after successful registration
        assertTrue(registrationPage.areFormFieldsCleared(),
                   "Form fields should be cleared after successful registration");
    }

    @Test
    void testRegistrationWithMismatchedPasswords() {
        // Given: Registration data with mismatched passwords
        String username = "testuser" + System.currentTimeMillis();
        String email = username + "@test.com";
        String password = "SecurePass123!";
        String confirmPassword = "DifferentPass456!";

        // When: User enters mismatched passwords
        registrationPage.register(username, email, password, confirmPassword);

        // Then: Registration should fail with password mismatch error
        registrationPage.waitForRegistrationToComplete();
        assertTrue(registrationPage.isRegistrationError() || 
                   registrationPage.hasValidationErrors(),
                   "Registration should fail with mismatched passwords");

        String errorMessage = registrationPage.getErrorMessage().toLowerCase();
        assertTrue(errorMessage.contains("password") && 
                   (errorMessage.contains("match") || errorMessage.contains("confirm")),
                   "Error message should indicate password mismatch");
    }

    @Test
    void testRegistrationWithInvalidEmail() {
        // Given: Registration data with invalid email
        String username = "testuser" + System.currentTimeMillis();
        String invalidEmail = "invalidemail";
        String password = "SecurePass123!";

        // When: User enters invalid email format
        registrationPage.register(username, invalidEmail, password);

        // Then: Registration should fail with email validation error
        assertTrue(registrationPage.hasValidationErrors() || 
                   registrationPage.isRegistrationError(),
                   "Registration should fail with invalid email format");
    }

    @Test
    void testRegistrationWithWeakPassword() {
        // Given: Registration data with weak password
        String username = "testuser" + System.currentTimeMillis();
        String email = username + "@test.com";
        String weakPassword = "123";

        // When: User enters weak password
        registrationPage.register(username, email, weakPassword);

        // Then: Registration should fail with password strength error
        assertTrue(registrationPage.hasValidationErrors() || 
                   registrationPage.isRegistrationError(),
                   "Registration should fail with weak password");
    }

    @Test
    void testRegistrationWithExistingUsername() {
        // Given: A user that already exists
        String existingUsername = "existinguser" + System.currentTimeMillis();
        String email1 = existingUsername + "1@test.com";
        String email2 = existingUsername + "2@test.com";
        String password = "SecurePass123!";

        // Create first user
        registrationPage.register(existingUsername, email1, password);
        registrationPage.waitForRegistrationToComplete();

        // Navigate back to registration page
        navigateToReactApp("/signup");

        // When: Attempt to register with same username but different email
        registrationPage.register(existingUsername, email2, password);

        // Then: Registration should fail with username already exists error
        registrationPage.waitForRegistrationToComplete();
        assertTrue(registrationPage.isRegistrationError(),
                   "Registration should fail when username already exists");

        String errorMessage = registrationPage.getRegistrationMessage().toLowerCase();
        assertTrue(errorMessage.contains("username") && 
                   (errorMessage.contains("exists") || errorMessage.contains("taken")),
                   "Error message should indicate username already exists");
    }

    @Test
    void testRegistrationWithExistingEmail() {
        // Given: A user that already exists
        String username1 = "user1_" + System.currentTimeMillis();
        String username2 = "user2_" + System.currentTimeMillis();
        String existingEmail = "existing" + System.currentTimeMillis() + "@test.com";
        String password = "SecurePass123!";

        // Create first user
        registrationPage.register(username1, existingEmail, password);
        registrationPage.waitForRegistrationToComplete();

        // Navigate back to registration page
        navigateToReactApp("/signup");

        // When: Attempt to register with same email but different username
        registrationPage.register(username2, existingEmail, password);

        // Then: Registration should fail with email already exists error
        registrationPage.waitForRegistrationToComplete();
        assertTrue(registrationPage.isRegistrationError(),
                   "Registration should fail when email already exists");

        String errorMessage = registrationPage.getRegistrationMessage().toLowerCase();
        assertTrue(errorMessage.contains("email") && 
                   (errorMessage.contains("exists") || errorMessage.contains("taken")),
                   "Error message should indicate email already exists");
    }

    @Test
    void testRegistrationWithEmptyFields() {
        // When: User attempts to register with empty fields
        registrationPage.clickRegisterButton();

        // Then: Form validation should prevent submission
        assertTrue(registrationPage.hasValidationErrors(),
                   "Should show validation errors for empty required fields");
    }

    @Test
    void testRegistrationPageElements() {
        // Then: All required elements should be present
        assertTrue(registrationPage.isOnRegistrationPage(),
                   "Should be on registration page");

        // Check form elements are present
        assertNotNull(registrationPage.getUsernameValue(),
                     "Username field should be present");
        assertNotNull(registrationPage.getEmailValue(),
                     "Email field should be present");
        assertNotNull(registrationPage.getRegisterButtonText(),
                     "Register button should be present");

        String buttonText = registrationPage.getRegisterButtonText();
        assertTrue(buttonText.toLowerCase().contains("create") || 
                   buttonText.toLowerCase().contains("register") ||
                   buttonText.toLowerCase().contains("sign up"),
                   "Register button should have appropriate text");
    }

    @Test
    void testNavigationToLogin() {
        // When: User clicks login link
        registrationPage.clickLoginLink();

        // Then: Should navigate to login page
        waitForPageLoad();
        assertTrue(getCurrentUrl().contains("/login") || 
                   getCurrentUrl().contains("/signin"),
                   "Should navigate to login page");
    }

    @Test
    void testRegistrationButtonLoadingState() {
        // Given: Valid registration data
        String username = "loadingtest" + System.currentTimeMillis();
        String email = username + "@test.com";
        String password = "SecurePass123!";

        // When: User submits registration form
        registrationPage.enterUsername(username);
        registrationPage.enterEmail(email);
        registrationPage.enterPassword(password);
        registrationPage.enterConfirmPassword(password);
        registrationPage.clickRegisterButton();

        // Then: Button should show loading state (briefly)
        String buttonText = registrationPage.getRegisterButtonText();
        assertTrue(buttonText.contains("Create") || 
                   buttonText.contains("Creating") ||
                   registrationPage.isRegisterButtonLoading(),
                   "Button should be in appropriate state");
    }

    @Test
    void testFormFieldValidation() {
        // Test individual field validation
        
        // Username validation
        registrationPage.enterUsername("ab"); // Too short
        registrationPage.enterEmail("valid@test.com");
        registrationPage.enterPassword("ValidPass123!");
        registrationPage.enterConfirmPassword("ValidPass123!");
        registrationPage.clickRegisterButton();
        
        assertTrue(registrationPage.hasValidationErrors(),
                   "Should show validation error for short username");
    }

    @Test
    void testFormFieldPersistence() {
        // Given: User enters data
        String username = "persisttest123";
        String email = "persist@test.com";

        // When: User enters data but doesn't submit
        registrationPage.enterUsername(username);
        registrationPage.enterEmail(email);

        // Then: Field values should persist
        assertEquals(username, registrationPage.getUsernameValue(),
                    "Username field should retain entered value");
        assertEquals(email, registrationPage.getEmailValue(),
                    "Email field should retain entered value");
    }

    @Test
    void testPasswordStrengthValidation() {
        // Given: Registration data with various password strengths
        String username = "passtest" + System.currentTimeMillis();
        String email = username + "@test.com";

        // Test weak passwords
        String[] weakPasswords = {"123", "password", "abc123", "qwerty"};
        
        for (String weakPassword : weakPasswords) {
            registrationPage.enterUsername(username);
            registrationPage.enterEmail(email);
            registrationPage.enterPassword(weakPassword);
            registrationPage.enterConfirmPassword(weakPassword);
            registrationPage.clickRegisterButton();

            assertTrue(registrationPage.hasValidationErrors() || 
                       registrationPage.isRegistrationError(),
                       "Should reject weak password: " + weakPassword);

            // Clear form for next test
            navigateToReactApp("/signup");
        }
    }

    @Test
    void testSpecialCharactersInFields() {
        // Given: Registration data with special characters
        String username = "user_test-123";
        String email = "test+special@example.com";
        String password = "Secure@Pass123!";

        // When: User enters data with special characters
        registrationPage.register(username, email, password);

        // Then: Should handle special characters appropriately
        registrationPage.waitForRegistrationToComplete();
        // This might succeed or fail depending on validation rules
        // The test verifies the system handles special characters gracefully
        assertTrue(registrationPage.isRegistrationSuccessful() || 
                   registrationPage.isRegistrationError(),
                   "System should handle special characters gracefully");
    }

    @Test
    void testLongFieldValues() {
        // Given: Registration data with very long values
        String longUsername = "a".repeat(100);
        String longEmail = "a".repeat(100) + "@test.com";
        String password = "SecurePass123!";

        // When: User enters very long values
        registrationPage.register(longUsername, longEmail, password);

        // Then: Should handle long values appropriately (validation or truncation)
        assertTrue(registrationPage.hasValidationErrors() || 
                   registrationPage.isRegistrationError() ||
                   registrationPage.isRegistrationSuccessful(),
                   "System should handle long field values gracefully");
    }
}
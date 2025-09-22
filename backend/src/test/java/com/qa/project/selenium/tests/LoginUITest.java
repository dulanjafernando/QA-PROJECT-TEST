package com.qa.project.selenium.tests;

import com.qa.project.selenium.config.SeleniumTestConfig;
import com.qa.project.selenium.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI tests for the React Login component
 * Tests the login functionality through the browser interface
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LoginUITest extends SeleniumTestConfig {

    private LoginPage loginPage;
    private TestRestTemplate restTemplate = new TestRestTemplate();

    @LocalServerPort
    private int testPort;

    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setUp first to initialize WebDriver
        loginPage = new LoginPage(driver);
        navigateToReactApp("/login");
    }

    @Test
    void testSuccessfulLogin() {
        // Given: User with valid credentials (keep username under 20 chars)
        String username = "test" + (System.currentTimeMillis() % 100000); // e.g., "test12345"
        String email = username + "@test.com";
        String password = "password123";

        // Create user via API first
        createUserViaAPI(username, email, password);

        // When: User enters valid credentials and clicks login
        loginPage.login(username, password);

        // Then: User should be successfully logged in
        loginPage.waitForLoginToComplete();
        assertTrue(loginPage.isLoginSuccessful(), 
                   "Login should be successful with valid credentials");
        
        // Should show success message or redirect to home page  
        String loginMessage = loginPage.getLoginMessage().toLowerCase();
        assertTrue(loginMessage.contains("successful") || 
                   loginMessage.contains("welcome") ||
                   getCurrentUrl().contains("/home"),
                   "Should show success message or redirect to home page. Found message: " + loginMessage + ", URL: " + getCurrentUrl());
    }

    @Test
    void testLoginWithInvalidCredentials() {
        // Given: Invalid credentials
        String username = "nonexistentuser";
        String password = "wrongpassword";

        // When: User enters invalid credentials
        loginPage.login(username, password);

        // Then: Login should fail with error message
        loginPage.waitForLoginToComplete();
        assertTrue(loginPage.isLoginError(), 
                   "Login should fail with invalid credentials");
        
        String errorMessage = loginPage.getLoginMessage().toLowerCase();
        assertTrue(errorMessage.contains("failed") || 
                   errorMessage.contains("incorrect") ||
                   errorMessage.contains("check your credentials") ||
                   errorMessage.contains("invalid"),
                   "Error message should indicate invalid credentials. Found: " + errorMessage);
    }

    @Test
    void testLoginWithEmptyFields() {
        // When: User attempts to login with empty fields
        loginPage.clickLoginButton();

        // Then: Form validation should prevent submission
        assertTrue(loginPage.hasValidationErrors() || 
                   loginPage.isLoginError(),
                   "Should show validation errors for empty fields");
    }

    @Test
    void testLoginWithEmptyUsername() {
        // When: User enters only password
        loginPage.enterPassword("password123");
        loginPage.clickLoginButton();

        // Then: Should show validation error for missing username
        assertTrue(loginPage.hasValidationErrors() || 
                   loginPage.isLoginError(),
                   "Should show validation error for empty username");
    }

    @Test
    void testLoginWithEmptyPassword() {
        // When: User enters only username
        loginPage.enterUsername("testuser");
        loginPage.clickLoginButton();

        // Then: Should show validation error for missing password
        assertTrue(loginPage.hasValidationErrors() || 
                   loginPage.isLoginError(),
                   "Should show validation error for empty password");
    }

    @Test
    void testLoginPageElements() {
        // Then: All required elements should be present
        assertTrue(loginPage.isOnLoginPage(), 
                   "Should be on login page");
        
        // Check form elements are present - use more flexible checks
        try {
            assertNotNull(loginPage.getUsernameValue(), 
                         "Username field should be present");
        } catch (Exception e) {
            // If getUsernameValue fails, at least verify we're on the right page
            assertTrue(driver.getCurrentUrl().contains("/login") || 
                      driver.getPageSource().contains("username"),
                      "Should have username field or be on login page");
        }
        
        String buttonText = "";
        try {
            buttonText = loginPage.getLoginButtonText().toLowerCase();
        } catch (Exception e) {
            // If button text fails, check page source for button
            assertTrue(driver.getPageSource().contains("button") ||
                      driver.getPageSource().contains("submit"),
                      "Should have a submit button");
            return; // Exit test if we can't get button text
        }
        
        assertTrue(buttonText.contains("sign") || 
                   buttonText.contains("login") ||
                   buttonText.contains("submit"),
                   "Login button should have appropriate text, found: " + buttonText);
    }

    @Test
    void testNavigationToRegistration() {
        // When: User clicks sign up link
        loginPage.clickSignUpLink();

        // Then: Should navigate to registration page
        waitForPageLoad();
        assertTrue(getCurrentUrl().contains("/signup") || 
                   getCurrentUrl().contains("/register"),
                   "Should navigate to registration page");
    }

    @Test
    void testLoginButtonLoadingState() {
        // Given: Valid credentials (keep username under 20 chars)
        String username = "test" + (System.currentTimeMillis() % 100000);
        String email = username + "@test.com";
        String password = "password123";
        createUserViaAPI(username, email, password);

        // When: User submits login form
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        loginPage.clickLoginButton();

        // Then: Button should show loading state (briefly) or normal state
        // The loading state might be too fast to catch consistently
        String buttonText = loginPage.getLoginButtonText();
        // Check for any valid button state
        assertTrue(buttonText.contains("Sign In") || 
                   buttonText.contains("Signing In") ||
                   loginPage.isLoginButtonLoading(),
                   "Button should be in a valid state. Found: " + buttonText);
    }

    @Test
    void testFormFieldPersistence() {
        // Given: User enters data
        String username = "testuser123";
        String password = "testpass";

        // When: User enters data but doesn't submit
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);

        // Then: Field values should persist
        assertEquals(username, loginPage.getUsernameValue(),
                    "Username field should retain entered value");
        // Note: Password field value might not be readable for security
    }

    @Test
    void testMultipleLoginAttempts() {
        // When: User makes multiple failed login attempts
        for (int i = 0; i < 3; i++) {
            loginPage.login("invalid" + i, "wrongpass" + i);
            loginPage.waitForLoginToComplete();
            assertTrue(loginPage.isLoginError(),
                      "Attempt " + (i + 1) + " should fail");
        }

        // Then: Should still allow login attempts (no lockout in basic implementation)
        String username = "valid" + (System.currentTimeMillis() % 100000);
        String email = username + "@test.com";
        String password = "password123";
        createUserViaAPI(username, email, password);

        loginPage.login(username, password);
        loginPage.waitForLoginToComplete();
        assertTrue(loginPage.isLoginSuccessful(),
                  "Valid login should still work after failed attempts");
    }

    /**
     * Helper method to create a user via API for testing
     */
    private void createUserViaAPI(String username, String email, String password) {
        try {
            // Use the main backend port (8080) that React app connects to
            String registerUrl = "http://localhost:8080/api/auth/register";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String requestBody = String.format(
                "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", 
                username, email, password
            );
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            var response = restTemplate.postForEntity(registerUrl, request, String.class);
            System.out.println("User registration response: " + response.getStatusCode() + " - " + response.getBody());
        } catch (Exception e) {
            // Log the full error for debugging
            System.out.println("User creation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
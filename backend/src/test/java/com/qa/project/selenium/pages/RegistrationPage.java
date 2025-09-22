package com.qa.project.selenium.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for the React Registration/SignUp page
 * Contains elements and actions related to user registration functionality
 */
public class RegistrationPage {
    
    private final WebDriver driver;
    private final WebDriverWait wait;

    // Registration form elements based on React SignUp component
    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "email")
    private WebElement emailField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(id = "confirmPassword")
    private WebElement confirmPasswordField;

    @FindBy(css = "button[type='submit']")
    private WebElement registerButton;

    @FindBy(css = ".message")
    private WebElement registerMessage;

    // Navigation elements
    @FindBy(linkText = "Login")
    private WebElement loginLink;

    // Page title elements
    @FindBy(css = ".signup-title")
    private WebElement signupTitle;

    @FindBy(css = ".signup-subtitle")
    private WebElement signupSubtitle;

    // Error message elements
    @FindBy(css = ".error-message")
    private WebElement errorMessage;

    public RegistrationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    /**
     * Enter username in the registration form
     * @param username the username to enter
     */
    public void enterUsername(String username) {
        wait.until(ExpectedConditions.visibilityOf(usernameField));
        usernameField.clear();
        usernameField.sendKeys(username);
    }

    /**
     * Enter email in the registration form
     * @param email the email to enter
     */
    public void enterEmail(String email) {
        wait.until(ExpectedConditions.visibilityOf(emailField));
        emailField.clear();
        emailField.sendKeys(email);
    }

    /**
     * Enter password in the registration form
     * @param password the password to enter
     */
    public void enterPassword(String password) {
        wait.until(ExpectedConditions.visibilityOf(passwordField));
        passwordField.clear();
        passwordField.sendKeys(password);
    }

    /**
     * Enter confirm password in the registration form
     * @param confirmPassword the confirm password to enter
     */
    public void enterConfirmPassword(String confirmPassword) {
        wait.until(ExpectedConditions.visibilityOf(confirmPasswordField));
        confirmPasswordField.clear();
        confirmPasswordField.sendKeys(confirmPassword);
    }

    /**
     * Click the register button
     */
    public void clickRegisterButton() {
        wait.until(ExpectedConditions.elementToBeClickable(registerButton));
        registerButton.click();
    }

    /**
     * Perform complete registration action
     * @param username the username
     * @param email the email
     * @param password the password
     * @param confirmPassword the confirm password
     */
    public void register(String username, String email, String password, String confirmPassword) {
        enterUsername(username);
        enterEmail(email);
        enterPassword(password);
        enterConfirmPassword(confirmPassword);
        clickRegisterButton();
    }

    /**
     * Perform registration with matching passwords
     * @param username the username
     * @param email the email
     * @param password the password
     */
    public void register(String username, String email, String password) {
        register(username, email, password, password);
    }

    /**
     * Get the registration message text
     * @return the message text
     */
    public String getRegistrationMessage() {
        wait.until(ExpectedConditions.visibilityOf(registerMessage));
        return registerMessage.getText();
    }

    /**
     * Check if registration message is displayed
     * @return true if message is visible
     */
    public boolean isRegistrationMessageDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(registerMessage));
            return registerMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if we're on the registration page
     * @return true if registration form is visible
     */
    public boolean isOnRegistrationPage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(signupTitle));
            return signupTitle.isDisplayed() && 
                   signupTitle.getText().contains("Create Account");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if registration message indicates success
     * @return true if message indicates success
     */
    public boolean isRegistrationSuccessful() {
        try {
            return isRegistrationMessageDisplayed() && 
                   getRegistrationMessage().toLowerCase().contains("successful") &&
                   registerMessage.getAttribute("class").contains("success");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if registration message indicates error
     * @return true if message indicates error
     */
    public boolean isRegistrationError() {
        try {
            return isRegistrationMessageDisplayed() && 
                   registerMessage.getAttribute("class").contains("error");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get error message text
     * @return error message text
     */
    public String getErrorMessage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(errorMessage));
            return errorMessage.getText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if form fields are cleared after successful registration
     * @return true if fields are empty
     */
    public boolean areFormFieldsCleared() {
        return usernameField.getAttribute("value").isEmpty() &&
               emailField.getAttribute("value").isEmpty() &&
               passwordField.getAttribute("value").isEmpty() &&
               confirmPasswordField.getAttribute("value").isEmpty();
    }

    /**
     * Get current username field value
     * @return username field value
     */
    public String getUsernameValue() {
        return usernameField.getAttribute("value");
    }

    /**
     * Get current email field value
     * @return email field value
     */
    public String getEmailValue() {
        return emailField.getAttribute("value");
    }

    /**
     * Navigate to login page using the link
     */
    public void clickLoginLink() {
        wait.until(ExpectedConditions.elementToBeClickable(loginLink));
        loginLink.click();
    }

    /**
     * Get the register button text
     * @return button text
     */
    public String getRegisterButtonText() {
        return registerButton.getText();
    }

    /**
     * Check if register button is in loading state
     * @return true if button shows loading
     */
    public boolean isRegisterButtonLoading() {
        try {
            return registerButton.getAttribute("class").contains("loading") ||
                   registerButton.getText().contains("Creating Account...");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for registration to complete (either success or error)
     */
    public void waitForRegistrationToComplete() {
        try {
            // Wait for either success message or error message to appear
            wait.until(ExpectedConditions.visibilityOf(registerMessage));
        } catch (Exception e) {
            // Timeout is acceptable here
        }
    }

    /**
     * Check if there are validation errors on the form
     * @return true if validation errors are present
     */
    public boolean hasValidationErrors() {
        try {
            return errorMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
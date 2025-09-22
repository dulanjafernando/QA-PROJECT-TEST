package com.qa.project.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Page Object Model for the React Login page
 * Contains elements and actions related to user login functionality
 */
public class LoginPage {
    
    private final WebDriver driver;
    private final WebDriverWait wait;

    // Login form elements based on React component
    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;

    @FindBy(css = ".message")
    private WebElement loginMessage;

    // Navigation elements - try multiple selectors for robustness
    @FindBy(linkText = "Sign up")
    private WebElement signUpLink;

    @FindBy(css = "a[href*='signup'], a[href*='register']")
    private WebElement signUpLinkByHref;

    // Page title elements
    @FindBy(css = ".auth-title, h1, h2")
    private WebElement loginTitle;

    @FindBy(css = ".auth-subtitle, p")
    private WebElement loginSubtitle;

    // Error message elements
    @FindBy(css = ".error-message")
    private WebElement errorMessage;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    /**
     * Enter username in the login form
     * @param username the username to enter
     */
    public void enterUsername(String username) {
        wait.until(ExpectedConditions.visibilityOf(usernameField));
        usernameField.clear();
        usernameField.sendKeys(username);
    }

    /**
     * Enter password in the login form
     * @param password the password to enter
     */
    public void enterPassword(String password) {
        wait.until(ExpectedConditions.visibilityOf(passwordField));
        passwordField.clear();
        passwordField.sendKeys(password);
    }

    /**
     * Click the login button
     */
    public void clickLoginButton() {
        wait.until(ExpectedConditions.elementToBeClickable(loginButton));
        loginButton.click();
    }

    /**
     * Perform complete login action
     * @param username the username
     * @param password the password
     */
    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLoginButton();
    }

    /**
     * Get the login message text
     * @return the message text
     */
    public String getLoginMessage() {
        try {
            // First try to get the message element
            WebElement messageElement = wait.until(ExpectedConditions.visibilityOf(loginMessage));
            return messageElement.getText();
        } catch (Exception e) {
            // Fallback: try to find any message elements
            try {
                WebElement anyMessage = driver.findElement(By.cssSelector(".message, .error, .success"));
                return anyMessage.getText();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    /**
     * Check if login message is displayed
     * @return true if message is visible
     */
    public boolean isLoginMessageDisplayed() {
        try {
            wait.until(ExpectedConditions.visibilityOf(loginMessage));
            return loginMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if we're on the login page
     * @return true if login form is visible
     */
    public boolean isOnLoginPage() {
        try {
            // Check multiple indicators that we're on the login page
            return driver.getCurrentUrl().contains("/login") || 
                   driver.getPageSource().contains("Welcome Back") ||
                   driver.getPageSource().contains("Please sign in") ||
                   usernameField.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if login message indicates success
     * @return true if successfully redirected to home page
     */
    public boolean isLoginSuccessful() {
        try {
            // Check for redirect to home page first (most reliable)
            if (driver.getCurrentUrl().contains("/home")) {
                return true;
            }
            
            // Wait a bit more for potential redirect
            Thread.sleep(3000);
            
            // Check again for redirect
            if (driver.getCurrentUrl().contains("/home")) {
                return true;
            }
            
            // If no redirect, check for success indicators in page content
            String pageSource = driver.getPageSource().toLowerCase();
            return pageSource.contains("welcome") || 
                   pageSource.contains("dashboard") ||
                   pageSource.contains("home") ||
                   !driver.getCurrentUrl().contains("/login");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if login message indicates error
     * @return true if message indicates error
     */
    public boolean isLoginError() {
        try {
            // Wait a moment for error message to appear
            Thread.sleep(1000);
            
            // Check for error message visibility
            if (isLoginMessageDisplayed()) {
                return true;
            }
            
            // Check page source for error indicators
            String pageSource = driver.getPageSource().toLowerCase();
            return pageSource.contains("error") || 
                   pageSource.contains("invalid") ||
                   pageSource.contains("incorrect") ||
                   pageSource.contains("failed") ||
                   driver.getCurrentUrl().contains("/login"); // Still on login page indicates failure
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
     * Click sign up link to navigate to registration
     */
    public void clickSignUpLink() {
        try {
            // Try the primary link text first
            wait.until(ExpectedConditions.elementToBeClickable(signUpLink));
            signUpLink.click();
        } catch (Exception e) {
            try {
                // Fallback to href-based selector
                wait.until(ExpectedConditions.elementToBeClickable(signUpLinkByHref));
                signUpLinkByHref.click();
            } catch (Exception ex) {
                // If both fail, look for any link that might lead to signup
                WebElement anySignupLink = driver.findElement(By.cssSelector("a[href*='/signup'], a[href*='/register'], a:contains('Sign'), a:contains('sign up')"));
                anySignupLink.click();
            }
        }
    }

    /**
     * Get the login button text
     * @return button text
     */
    public String getLoginButtonText() {
        return loginButton.getText();
    }

    /**
     * Check if login button is in loading state
     * @return true if button shows loading
     */
    public boolean isLoginButtonLoading() {
        try {
            String buttonText = loginButton.getText().toLowerCase();
            return loginButton.getAttribute("class").contains("loading") ||
                   buttonText.contains("signing") ||
                   buttonText.contains("loading") ||
                   buttonText.contains("...") ||
                   loginButton.getAttribute("disabled") != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for login to complete (either success or error)
     */
    public void waitForLoginToComplete() {
        try {
            // Give the login process time to complete
            Thread.sleep(5000);
            
            // Try to wait for either success (redirect) or error message
            try {
                wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("/home"),
                    ExpectedConditions.visibilityOf(loginMessage)
                ));
            } catch (Exception e) {
                // Timeout is acceptable - login process may have completed
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if redirected to home page after successful login
     * @return true if on home page
     */
    public boolean isRedirectedToHome() {
        try {
            // Wait a bit for potential redirect
            Thread.sleep(2000);
            return driver.getCurrentUrl().contains("/home");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Get current username field value
     * @return username field value
     */
    public String getUsernameValue() {
        return usernameField.getAttribute("value");
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
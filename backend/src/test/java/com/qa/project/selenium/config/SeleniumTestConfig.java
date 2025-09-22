package com.qa.project.selenium.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

/**
 * Base configuration class for Selenium UI tests
 * Provides WebDriver setup, configuration, and common test utilities
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class SeleniumTestConfig {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;

    @LocalServerPort
    protected int port;

    @BeforeEach
    public void setUp() {
        // Setup WebDriverManager to automatically manage driver binaries
        WebDriverManager.chromedriver().setup();
        
        // Configure Chrome options for testing
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=VizDisplayCompositor");
        
        // Run in headless mode for CI/CD environments
        // Comment out the next line if you want to see the browser during development
        options.addArguments("--headless");
        
        // Initialize WebDriver
        driver = new ChromeDriver(options);
        
        // Configure implicit wait and explicit wait
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        
        // Set window size for consistent testing
        driver.manage().window().maximize();
        
        // Construct base URL for the application
        baseUrl = "http://localhost:" + port;
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Navigate to a specific path relative to the base URL
     * @param path the relative path to navigate to
     */
    protected void navigateTo(String path) {
        driver.get(baseUrl + path);
    }

    /**
     * Get the current page URL
     * @return current URL
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Get the page title
     * @return page title
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Navigate to React app path (assumes React app runs on port 3000 or 5173)
     * @param path the relative path to navigate to in React app
     */
    protected void navigateToReactApp(String path) {
        // Try common Vite/React development ports in order
        String[] ports = {"5174", "5173", "3000"};
        
        for (String port : ports) {
            try {
                String reactUrl = "http://localhost:" + port + path;
                driver.get(reactUrl);
                
                // Give page time to load
                Thread.sleep(2000);
                
                // Check if page loaded successfully by looking for React content
                if (driver.getPageSource().contains("auth-container") || 
                    driver.getPageSource().contains("Welcome Back") ||
                    driver.getPageSource().contains("Create Account") ||
                    driver.getTitle().contains("React") ||
                    !driver.getPageSource().contains("This site can't be reached")) {
                    return; // Successfully loaded
                }
            } catch (Exception e) {
                // Try next port
                continue;
            }
        }
        
        // If all ports failed, throw exception
        throw new RuntimeException("Could not connect to React app on any of the common ports: " + 
                                 String.join(", ", ports));
    }

    /**
     * Wait for page to load completely
     */
    protected void waitForPageLoad() {
        wait.until(driver -> ((org.openqa.selenium.JavascriptExecutor) driver)
            .executeScript("return document.readyState").equals("complete"));
    }

    /**
     * Wait for a specific amount of time (use sparingly, prefer explicit waits)
     * @param milliseconds time to wait
     */
    protected void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
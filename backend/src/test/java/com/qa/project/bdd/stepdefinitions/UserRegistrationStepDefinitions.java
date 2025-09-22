package com.qa.project.bdd.stepdefinitions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.project.bdd.shared.SharedResponseHolder;
import com.qa.project.dto.AuthResponse;
import com.qa.project.dto.RegisterRequest;
import com.qa.project.model.User;
import com.qa.project.repository.UserRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for User Registration BDD scenarios.
 * Maps Gherkin steps to Java test implementation for registration features.
 */
public class UserRegistrationStepDefinitions {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SharedResponseHolder sharedResponseHolder;

    // Shared state for test scenarios
    private RegisterRequest currentRegisterRequest;
    private AuthResponse lastAuthResponse;

    @Given("I want to register a new user account")
    public void i_want_to_register_a_new_user_account() {
        // Initialize registration context
        currentRegisterRequest = new RegisterRequest();
        lastAuthResponse = null;
    }

    @Given("the database is clean")
    public void the_database_is_clean() {
        // Clear all users from the database
        userRepository.deleteAll();
    }

    @Given("the application is running")
    public void the_application_is_running() {
        // This step verifies that the Spring Boot application context is loaded
        // The application is automatically started by @SpringBootTest
        // This step serves as a readable BDD precondition
    }

    @Given("a user with username {string} already exists")
    public void a_user_with_username_already_exists(String username) {
        // Create an existing user with the specified username
        User existingUser = new User();
        existingUser.setUsername(username);
        existingUser.setEmail("existing@example.com");
        existingUser.setPassword("$2a$10$HashedPassword"); // BCrypt hashed
        userRepository.save(existingUser);
    }

    @Given("a user with email {string} already exists")
    public void a_user_with_email_already_exists(String email) {
        // Create an existing user with the specified email
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail(email);
        existingUser.setPassword("$2a$10$HashedPassword"); // BCrypt hashed
        userRepository.save(existingUser);
    }

    @When("I provide valid registration details:")
    public void i_provide_valid_registration_details(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps();
        Map<String, String> userDetails = users.get(0);
        
        currentRegisterRequest = new RegisterRequest();
        currentRegisterRequest.setUsername(userDetails.get("username"));
        currentRegisterRequest.setEmail(userDetails.get("email"));
        currentRegisterRequest.setPassword(userDetails.get("password"));
        
        performRegistrationRequest();
    }

    @When("I try to register with the same username:")
    public void i_try_to_register_with_the_same_username(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps();
        Map<String, String> userDetails = users.get(0);
        
        currentRegisterRequest = new RegisterRequest();
        currentRegisterRequest.setUsername(userDetails.get("username"));
        currentRegisterRequest.setEmail(userDetails.get("email"));
        currentRegisterRequest.setPassword(userDetails.get("password"));
        
        performRegistrationRequest();
    }

    @When("I try to register with the same email:")
    public void i_try_to_register_with_the_same_email(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps();
        Map<String, String> userDetails = users.get(0);
        
        currentRegisterRequest = new RegisterRequest();
        currentRegisterRequest.setUsername(userDetails.get("username"));
        currentRegisterRequest.setEmail(userDetails.get("email"));
        currentRegisterRequest.setPassword(userDetails.get("password"));
        
        performRegistrationRequest();
    }

    @When("I provide invalid registration details:")
    public void i_provide_invalid_registration_details(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps();
        Map<String, String> userDetails = users.get(0);
        
        currentRegisterRequest = new RegisterRequest();
        currentRegisterRequest.setUsername(convertEmptyValue(userDetails.get("username")));
        currentRegisterRequest.setEmail(convertEmptyValue(userDetails.get("email")));
        currentRegisterRequest.setPassword(convertEmptyValue(userDetails.get("password")));
        
        performRegistrationRequest();
    }
    
    // Helper method to convert [empty] values to null
    private String convertEmptyValue(String value) {
        if ("[empty]".equals(value)) {
            return null;
        }
        return value;
    }

    @When("I provide a username longer than {int} characters:")
    public void i_provide_a_username_longer_than_characters(Integer maxLength, DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps();
        Map<String, String> userDetails = users.get(0);
        
        currentRegisterRequest = new RegisterRequest();
        currentRegisterRequest.setUsername(userDetails.get("username"));
        currentRegisterRequest.setEmail(userDetails.get("email"));
        currentRegisterRequest.setPassword(userDetails.get("password"));
        
        performRegistrationRequest();
    }

    @Then("the registration should be successful")
    public void the_registration_should_be_successful() {
        assertEquals(200, getLastResponseStatus(), 
            "Registration should return 200 OK status");
        
        assertNotNull(lastAuthResponse, "Response should contain authentication data");
        assertTrue(lastAuthResponse.isSuccess(), "Registration should be marked as successful");
    }

    @Then("the registration should fail")
    public void the_registration_should_fail() {
        assertTrue(getLastResponseStatus() >= 400, 
            "Registration should return error status (4xx or 5xx)");
        
        if (lastAuthResponse != null) {
            assertFalse(lastAuthResponse.isSuccess(), "Registration should be marked as failed");
        }
    }

    @Then("I should receive an error message {string}")
    public void i_should_receive_an_error_message(String expectedMessage) {
        // Check if we have a parsed AuthResponse with error message
        if (lastAuthResponse != null && lastAuthResponse.getMessage() != null) {
            String actualMessage = lastAuthResponse.getMessage();
            
            // Special handling for combined validation messages
            if (expectedMessage.equals("Username and password are required")) {
                boolean hasUsernameError = actualMessage.contains("Username is required");
                boolean hasPasswordError = actualMessage.contains("Password is required");
                assertTrue(hasUsernameError && hasPasswordError,
                          "Response should contain both username and password validation errors, but got: " + actualMessage);
            } else {
                assertTrue(actualMessage.contains(expectedMessage),
                          "Response should contain expected error message: " + expectedMessage + 
                          ", but got: " + actualMessage);
            }
            return;
        }
        
        // Fallback to raw response body
        String responseBody = getLastResponseBody();
        assertNotNull(responseBody, "Response should contain error message");
        
        // Special handling for combined validation messages in raw response
        if (expectedMessage.equals("Username and password are required")) {
            boolean hasUsernameError = responseBody.contains("Username is required");
            boolean hasPasswordError = responseBody.contains("Password is required");
            assertTrue(hasUsernameError && hasPasswordError,
                      "Response should contain both username and password validation errors, but got: " + responseBody);
        } else {
            assertTrue(responseBody.contains(expectedMessage),
                      "Response should contain expected error message: " + expectedMessage + 
                      ", but got: " + responseBody);
        }
    }

    @Then("the user should be saved in the database")
    public void the_user_should_be_saved_in_the_database() {
        assertNotNull(currentRegisterRequest, "Registration request should exist");
        
        // Verify user exists in database
        User savedUser = userRepository.findByUsername(currentRegisterRequest.getUsername()).orElse(null);
        assertNotNull(savedUser, "User should be saved in database");
        assertEquals(currentRegisterRequest.getUsername(), savedUser.getUsername());
        assertEquals(currentRegisterRequest.getEmail(), savedUser.getEmail());
        
        // Verify password is encrypted (not plain text)
        assertNotEquals(currentRegisterRequest.getPassword(), savedUser.getPassword(),
            "Password should be encrypted in database");
    }

    // Helper method to perform registration HTTP request
    private void performRegistrationRequest() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<RegisterRequest> request = new HttpEntity<>(currentRegisterRequest, headers);
            setLastRequestPayload(currentRegisterRequest);
            
            String url = getBaseUrl() + "/register";
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            setLastResponse(response.getBody(), response.getStatusCode().value());
            sharedResponseHolder.setLastResponse(response);
            
            // Always try to parse response as AuthResponse
            if (response.getBody() != null) {
                try {
                    lastAuthResponse = objectMapper.readValue(response.getBody(), AuthResponse.class);
                } catch (Exception e) {
                    // If parsing fails, create error response
                    lastAuthResponse = new AuthResponse(response.getBody(), false);
                }
            }
            
        } catch (Exception e) {
            // Handle connection errors or other exceptions
            setLastResponse("Connection error: " + e.getMessage(), 500);
        }
    }

    // Utility methods for managing test state
    private void setLastRequestPayload(Object payload) {
        // Store the last request payload for debugging purposes
        // This method tracks what data was sent in the request
    }

    private String getBaseUrl() {
        return "/api/auth";
    }

    private void setLastResponse(String body, int statusCode) {
        // This method is kept for compatibility but we use sharedResponseHolder now
    }

    private int getLastResponseStatus() {
        return sharedResponseHolder.getLastResponseStatus();
    }

    private String getLastResponseBody() {
        return sharedResponseHolder.getLastResponseBody();
    }
}
package com.qa.project.bdd.stepdefinitions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.project.bdd.shared.SharedResponseHolder;
import com.qa.project.dto.AuthResponse;
import com.qa.project.dto.LoginRequest;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for User Login BDD scenarios.
 * Maps Gherkin steps to Java test implementation for authentication features.
 */
public class UserLoginStepDefinitions {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private SharedResponseHolder sharedResponseHolder;

    // Shared state for test scenarios
    private LoginRequest currentLoginRequest;
    private AuthResponse lastAuthResponse;
    private String lastAuthToken;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Given("the following users exist:")
    public void the_following_users_exist(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps();
        
        for (Map<String, String> userData : users) {
            User user = new User();
            user.setUsername(userData.get("username"));
            user.setEmail(userData.get("email"));
            // Encrypt password before saving
            user.setPassword(passwordEncoder.encode(userData.get("password")));
            userRepository.save(user);
        }
    }

    @Given("I want to login to my account")
    public void i_want_to_login_to_my_account() {
        // Initialize login context
        currentLoginRequest = new LoginRequest();
        lastAuthResponse = null;
        lastAuthToken = null;
    }

    @When("I provide valid login credentials:")
    public void i_provide_valid_login_credentials(DataTable dataTable) {
        List<Map<String, String>> credentials = dataTable.asMaps();
        Map<String, String> loginData = credentials.get(0);
        
        currentLoginRequest = new LoginRequest();
        currentLoginRequest.setUsername(loginData.get("username"));
        currentLoginRequest.setPassword(loginData.get("password"));
        
        performLoginRequest();
    }

    @When("I provide valid login credentials using email:")
    public void i_provide_valid_login_credentials_using_email(DataTable dataTable) {
        List<Map<String, String>> credentials = dataTable.asMaps();
        Map<String, String> loginData = credentials.get(0);
        
        currentLoginRequest = new LoginRequest();
        // Use email as username field (our system should support this)
        currentLoginRequest.setUsername(loginData.get("email"));
        currentLoginRequest.setPassword(loginData.get("password"));
        
        performLoginRequest();
    }

    @When("I provide incorrect login credentials:")
    public void i_provide_incorrect_login_credentials(DataTable dataTable) {
        List<Map<String, String>> credentials = dataTable.asMaps();
        Map<String, String> loginData = credentials.get(0);
        
        currentLoginRequest = new LoginRequest();
        currentLoginRequest.setUsername(loginData.get("username"));
        currentLoginRequest.setPassword(loginData.get("password"));
        
        performLoginRequest();
    }

    @When("I provide login credentials for non-existent user:")
    public void i_provide_login_credentials_for_non_existent_user(DataTable dataTable) {
        List<Map<String, String>> credentials = dataTable.asMaps();
        Map<String, String> loginData = credentials.get(0);
        
        currentLoginRequest = new LoginRequest();
        currentLoginRequest.setUsername(loginData.get("username"));
        currentLoginRequest.setPassword(loginData.get("password"));
        
        performLoginRequest();
    }

    @When("I provide incomplete login credentials:")
    public void i_provide_incomplete_login_credentials(DataTable dataTable) {
        List<Map<String, String>> credentials = dataTable.asMaps();
        Map<String, String> loginData = credentials.get(0);
        
        currentLoginRequest = new LoginRequest();
        currentLoginRequest.setUsername(convertEmptyValue(loginData.get("username")));
        currentLoginRequest.setPassword(convertEmptyValue(loginData.get("password")));
        
        performLoginRequest();
    }
    
    // Helper method to convert [empty] values to null
    private String convertEmptyValue(String value) {
        if ("[empty]".equals(value)) {
            return null;
        }
        return value;
    }

    @When("I login successfully with valid credentials:")
    public void i_login_successfully_with_valid_credentials(DataTable dataTable) {
        i_provide_valid_login_credentials(dataTable);
        the_login_should_be_successful();
    }

    @When("I login again with the same credentials:")
    public void i_login_again_with_the_same_credentials(DataTable dataTable) {
        // Save previous response for comparison
        AuthResponse previousResponse = lastAuthResponse;
        String previousToken = lastAuthToken;
        
        // Perform login again
        i_provide_valid_login_credentials(dataTable);
        
        // Both should be successful
        the_login_should_be_successful();
        
        // Verify both responses were successful
        assertNotNull(previousResponse, "Previous login should have been successful");
        assertTrue(previousResponse.isSuccess(), "Previous login should be marked as successful");
    }

    @Then("the login should be successful")
    public void the_login_should_be_successful() {
        assertEquals(200, getLastResponseStatus(), 
            "Login should return 200 OK status");
        
        assertNotNull(lastAuthResponse, "Response should contain authentication data");
        assertTrue(lastAuthResponse.isSuccess(), "Login should be marked as successful");
    }

    @Then("the login should fail")
    public void the_login_should_fail() {
        assertTrue(getLastResponseStatus() >= 400, 
            "Login should return error status (4xx or 5xx)");
        
        if (lastAuthResponse != null) {
            assertFalse(lastAuthResponse.isSuccess(), "Login should be marked as failed");
        }
    }

    @Then("I should receive an authentication token")
    public void i_should_receive_an_authentication_token() {
        assertNotNull(lastAuthResponse, "Response should contain authentication data");
        assertNotNull(lastAuthResponse.getToken(), "Response should contain authentication token");
        assertFalse(lastAuthResponse.getToken().isEmpty(), "Authentication token should not be empty");
        lastAuthToken = lastAuthResponse.getToken();
    }

    @Then("no authentication token should be provided")
    public void no_authentication_token_should_be_provided() {
        if (lastAuthResponse != null) {
            assertTrue(lastAuthResponse.getToken() == null || lastAuthResponse.getToken().isEmpty(),
                "No authentication token should be provided for failed login");
        }
    }

    @Then("both login attempts should be successful")
    public void both_login_attempts_should_be_successful() {
        // This is already verified by calling the_login_should_be_successful() twice
        assertNotNull(lastAuthResponse, "Second login should be successful");
        assertTrue(lastAuthResponse.isSuccess(), "Second login should be marked as successful");
    }

    @Then("I should receive authentication tokens for both attempts")
    public void i_should_receive_authentication_tokens_for_both_attempts() {
        i_should_receive_an_authentication_token();
        // Additional verification that token is valid
        assertNotNull(lastAuthToken, "Should have authentication token");
        assertTrue(lastAuthToken.length() > 10, "Token should be substantial length");
    }

    @Then("the returned user data should contain:")
    public void the_returned_user_data_should_contain(DataTable dataTable) {
        assertNotNull(lastAuthResponse, "Response should contain user data");
        
        List<Map<String, String>> expectedData = dataTable.asMaps();
        for (Map<String, String> row : expectedData) {
            String field = row.get("field");
            String expectedValue = row.get("value");
            
            switch (field) {
                case "username":
                    assertEquals(expectedValue, lastAuthResponse.getUsername(),
                        "Username should match expected value");
                    break;
                case "email":
                    assertEquals(expectedValue, lastAuthResponse.getEmail(),
                        "Email should match expected value");
                    break;
                default:
                    fail("Unknown field: " + field);
            }
        }
    }

    @Then("the password should not be exposed in the response")
    public void the_password_should_not_be_exposed_in_the_response() {
        String responseBody = getLastResponseBody();
        assertNotNull(responseBody, "Response body should exist");
        
        // Verify response doesn't contain password-related fields
        assertFalse(responseBody.toLowerCase().contains("password"),
            "Response should not contain password field");
        assertFalse(responseBody.contains("$2a$"), // BCrypt hash prefix
            "Response should not contain password hash");
    }

    @Then("I should receive a success message {string}")
    public void i_should_receive_a_success_message(String expectedMessage) {
        String responseBody = getLastResponseBody();
        assertNotNull(responseBody, "Response body should exist");
        
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // Check for success message in different possible fields
            String message = null;
            if (jsonResponse.has("message")) {
                message = jsonResponse.get("message").asText();
            } else if (jsonResponse.has("status")) {
                message = jsonResponse.get("status").asText();
            } else if (jsonResponse.has("response")) {
                message = jsonResponse.get("response").asText();
            }
            
            if (message != null) {
                assertEquals(expectedMessage, message, 
                    "Expected success message: '" + expectedMessage + "', but got: '" + message + "'");
            } else {
                // If no message field found, check if response contains expected text
                assertTrue(responseBody.contains(expectedMessage),
                    "Response should contain success message: '" + expectedMessage + "'. Actual response: " + responseBody);
            }
        } catch (Exception e) {
            // Fallback: simple string contains check
            assertTrue(responseBody.contains(expectedMessage),
                "Response should contain success message: '" + expectedMessage + "'. Actual response: " + responseBody);
        }
    }

    // Helper method to perform login HTTP request
    private void performLoginRequest() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<LoginRequest> request = new HttpEntity<>(currentLoginRequest, headers);
            setLastRequestPayload(currentLoginRequest);
            
            String url = getBaseUrl() + "/login";
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
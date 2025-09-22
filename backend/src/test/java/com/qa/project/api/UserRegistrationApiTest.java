package com.qa.project.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * REST Assured API tests for user registration endpoint
 * Tests the /api/auth/register endpoint with various scenarios
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("User Registration API Tests")
public class UserRegistrationApiTest extends BaseApiTest {

    @Test
    @DisplayName("Should successfully register a new user with valid data")
    public void testSuccessfulUserRegistration() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Use last 5 digits
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "test" + timestamp);
        userData.put("email", "test" + timestamp + "@example.com");
        userData.put("password", "SecurePassword123!");

        given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("User registered successfully"))
                .body("success", equalTo(true))
                .body("userId", notNullValue())
                .body("userId", instanceOf(Number.class));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    public void testRegistrationWithDuplicateEmail() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8); // Use last 5 digits
        String email = "dup" + timestamp + "@example.com";
        
        // First registration - should succeed
        Map<String, String> firstUser = new HashMap<>();
        firstUser.put("username", "first" + timestamp);
        firstUser.put("email", email);
        firstUser.put("password", "Password123!");

        given()
                .spec(requestSpec)
                .body(firstUser)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(200);

        // Second registration with same email - should fail
        Map<String, String> secondUser = new HashMap<>();
        secondUser.put("username", "second" + timestamp);
        secondUser.put("email", email); // Same email
        secondUser.put("password", "Password456!");

        given()
                .spec(requestSpec)
                .body(secondUser)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", containsString("Email already exists"))
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject registration with invalid email format")
    public void testRegistrationWithInvalidEmail() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "test" + timestamp);
        userData.put("email", "invalid-email-format");
        userData.put("password", "Password123!");

        given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject registration with missing required fields")
    public void testRegistrationWithMissingFields() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        
        // Test missing username
        Map<String, String> missingUsername = new HashMap<>();
        missingUsername.put("email", "test" + timestamp + "@example.com");
        missingUsername.put("password", "Password123!");

        given()
                .spec(requestSpec)
                .body(missingUsername)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));

        // Test missing email
        Map<String, String> missingEmail = new HashMap<>();
        missingEmail.put("username", "test" + timestamp);
        missingEmail.put("password", "Password123!");

        given()
                .spec(requestSpec)
                .body(missingEmail)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));

        // Test missing password
        Map<String, String> missingPassword = new HashMap<>();
        missingPassword.put("username", "test" + timestamp);
        missingPassword.put("email", "test" + timestamp + "b@example.com");

        given()
                .spec(requestSpec)
                .body(missingPassword)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject registration with empty request body")
    public void testRegistrationWithEmptyBody() {
        given()
                .spec(requestSpec)
                .body("{}")
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject registration with weak password")
    public void testRegistrationWithWeakPassword() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "test" + timestamp);
        userData.put("email", "test" + timestamp + "@example.com");
        userData.put("password", "123"); // Weak password

        given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should validate response payload structure for successful registration")
    public void testRegistrationResponsePayloadStructure() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "payload" + timestamp);
        userData.put("email", "payload" + timestamp + "@example.com");
        userData.put("password", "SecurePassword123!");

        Response response = given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(200)
                .extract().response();

        // Validate response structure
        response.then()
                .body("$", hasKey("message"))
                .body("$", hasKey("success"))
                .body("$", hasKey("userId"))
                .body("success", equalTo(true))
                .body("message", notNullValue())
                .body("userId", instanceOf(Number.class));
    }

    @Test
    @DisplayName("Should handle special characters in username")
    public void testRegistrationWithSpecialCharactersInUsername() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "test@user#" + timestamp);
        userData.put("email", "special" + timestamp + "@example.com");
        userData.put("password", "Password123!");

        // This might be accepted or rejected depending on business rules
        Response response = given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register");

        // Check that we get a proper response (either success or proper error)
        response.then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .contentType("application/json");

        if (response.statusCode() == 400) {
            response.then()
                    .body("message", notNullValue())
                    .body("success", equalTo(false));
        } else {
            response.then()
                    .body("success", equalTo(true))
                    .body("userId", notNullValue());
        }
    }
}
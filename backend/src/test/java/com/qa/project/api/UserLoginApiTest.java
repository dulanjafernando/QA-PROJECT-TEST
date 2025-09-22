package com.qa.project.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * REST Assured API tests for user login endpoint
 * Tests the /api/auth/login endpoint with various scenarios
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("User Login API Tests")
public class UserLoginApiTest extends BaseApiTest {

    private String testUsername;
    private String testEmail;
    private String testPassword;

    @BeforeEach
    public void setupTestUser() {
        // Create a unique test user for each test
        String uniqueId = String.valueOf(System.currentTimeMillis()).substring(8);
        testUsername = "login" + uniqueId;
        testEmail = "logintest" + uniqueId + "@example.com";
        testPassword = "TestPassword123!";

        // Register the test user
        Map<String, String> userData = new HashMap<>();
        userData.put("username", testUsername);
        userData.put("email", testEmail);
        userData.put("password", testPassword);

        given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    public void testSuccessfulLogin() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", testUsername);
        loginData.put("password", testPassword);

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("message", equalTo("Login successful"))
                .body("success", equalTo(true))
                .body("userId", notNullValue())
                .body("userId", instanceOf(Number.class));
    }

    @Test
    @DisplayName("Should reject login with invalid username")
    public void testLoginWithInvalidEmail() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "nonexistentuser");
        loginData.put("password", testPassword);

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .contentType("application/json")
                .body("message", containsString("Invalid username or password"))
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    public void testLoginWithInvalidPassword() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", testUsername);
        loginData.put("password", "WrongPassword123!");

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .contentType("application/json")
                .body("message", containsString("Invalid username or password"))
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject login with missing username field")
    public void testLoginWithMissingEmail() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("password", testPassword);

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject login with missing password field")
    public void testLoginWithMissingPassword() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", testUsername);

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject login with empty request body")
    public void testLoginWithEmptyBody() {
        given()
                .spec(requestSpec)
                .body("{}")
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject login with empty username")
    public void testLoginWithEmptyEmail() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "");
        loginData.put("password", testPassword);

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should reject login with empty password")
    public void testLoginWithEmptyPassword() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", testUsername);
        loginData.put("password", "");

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should validate response payload structure for successful login")
    public void testLoginResponsePayloadStructure() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", testUsername);
        loginData.put("password", testPassword);

        Response response = given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(200)
                .extract().response();

        // Validate response structure
        response.then()
                .body("$", hasKey("message"))
                .body("$", hasKey("success"))
                .body("$", hasKey("userId"))
                .body("success", equalTo(true))
                .body("message", instanceOf(String.class))
                .body("userId", instanceOf(Number.class));
    }

    @Test
    @DisplayName("Should handle malformed username format in login")
    public void testLoginWithMalformedEmail() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "malformed-username@@@");
        loginData.put("password", testPassword);

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(401)))
                .contentType("application/json")
                .body("message", notNullValue())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should handle case sensitivity in username")
    public void testLoginWithDifferentEmailCase() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", testUsername.toUpperCase()); // Different case
        loginData.put("password", testPassword);

        Response response = given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login");

        // Should either succeed (case insensitive) or fail gracefully
        response.then()
                .statusCode(anyOf(equalTo(200), equalTo(401)))
                .contentType("application/json");

        if (response.statusCode() == 401) {
            response.then()
                    .body("message", notNullValue())
                    .body("success", equalTo(false));
        } else {
            response.then()
                    .body("success", equalTo(true))
                    .body("userId", notNullValue());
        }
    }

    @Test
    @DisplayName("Should validate response time for login request")
    public void testLoginResponseTime() {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", testUsername);
        loginData.put("password", testPassword);

        given()
                .spec(requestSpec)
                .body(loginData)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(200)
                .time(lessThan(5000L)); // Should respond within 5 seconds
    }
}
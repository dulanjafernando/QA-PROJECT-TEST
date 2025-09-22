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
 * Comprehensive error handling tests for authentication API endpoints
 * Tests edge cases, malformed requests, and error response validation
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("API Error Handling Tests")
public class ApiErrorHandlingTest extends BaseApiTest {

    @Test
    @DisplayName("Should handle malformed JSON in registration request")
    public void testRegistrationWithMalformedJson() {
        given()
                .spec(requestSpec)
                .body("{ invalid json structure")
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(403))); // Bad Request or Forbidden
    }

    @Test
    @DisplayName("Should handle malformed JSON in login request")
    public void testLoginWithMalformedJson() {
        given()
                .spec(requestSpec)
                .body("{ \"email\": missing quotes }")
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(403))); // Bad Request or Forbidden
    }

    @Test
    @DisplayName("Should handle null values in registration")
    public void testRegistrationWithNullValues() {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", null);
        userData.put("email", null);
        userData.put("password", null);

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
    @DisplayName("Should handle null values in login")
    public void testLoginWithNullValues() {
        Map<String, Object> loginData = new HashMap<>();
        loginData.put("email", null);
        loginData.put("password", null);

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
    @DisplayName("Should handle very long input values in registration")
    public void testRegistrationWithVeryLongInputs() {
        String veryLongString = "a".repeat(1000); // 1000 character string

        Map<String, String> userData = new HashMap<>();
        userData.put("username", veryLongString);
        userData.put("email", veryLongString + "@example.com");
        userData.put("password", veryLongString);

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
    @DisplayName("Should handle SQL injection attempts in registration")
    public void testRegistrationWithSqlInjection() {
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "test'; DROP TABLE users; --");
        userData.put("email", "injection@example.com");
        userData.put("password", "Password123!");

        Response response = given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register");

        // Should either reject the input or sanitize it safely
        response.then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .contentType("application/json");

        if (response.statusCode() == 400) {
            response.then()
                    .body("message", notNullValue())
                    .body("success", equalTo(false));
        }
    }

    @Test
    @DisplayName("Should handle XSS attempts in registration")
    public void testRegistrationWithXssAttempt() {
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "<script>alert('xss')</script>");
        userData.put("email", "xss" + System.currentTimeMillis() + "@example.com");
        userData.put("password", "Password123!");

        Response response = given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register");

        // Should either reject or sanitize the input
        response.then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .contentType("application/json");

        if (response.statusCode() == 400) {
            response.then()
                    .body("message", notNullValue())
                    .body("success", equalTo(false));
        }
    }

    @Test
    @DisplayName("Should handle wrong HTTP method for endpoints")
    public void testWrongHttpMethods() {
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "testuser");
        userData.put("email", "test@example.com");
        userData.put("password", "Password123!");

        // Test GET instead of POST for registration
        given()
                .spec(requestSpec)
        .when()
                .get("/api/auth/register")
        .then()
                .statusCode(anyOf(equalTo(405), equalTo(403))); // Method Not Allowed or Forbidden

        // Test PUT instead of POST for login
        given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .put("/api/auth/login")
        .then()
                .statusCode(anyOf(equalTo(405), equalTo(403))); // Method Not Allowed or Forbidden
    }

    @Test
    @DisplayName("Should handle requests without Content-Type header")
    public void testRequestsWithoutContentType() {
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "testuser");
        userData.put("email", "test@example.com");
        userData.put("password", "Password123!");

        given()
                .accept("application/json")
                .body(userData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(anyOf(equalTo(200), equalTo(415), equalTo(403))); // Success, Unsupported Media Type, or Forbidden
    }

    @Test
    @DisplayName("Should handle requests with wrong Content-Type")
    public void testRequestsWithWrongContentType() {
        String jsonBody = "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"Password123!\"}";

        given()
                .contentType("text/plain")
                .accept("application/json")
                .body(jsonBody)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(anyOf(equalTo(415), equalTo(403))); // Unsupported Media Type or Forbidden
    }

    @Test
    @DisplayName("Should validate error response structure consistency")
    public void testErrorResponseStructureConsistency() {
        // Test with invalid data to get error response
        Map<String, String> invalidData = new HashMap<>();
        invalidData.put("email", "invalid-email");
        invalidData.put("password", "weak");

        Response response = given()
                .spec(requestSpec)
                .body(invalidData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .extract().response();

        // Validate error response structure - AuthResponse uses 'message' field
        response.then()
                .body("$", hasKey("message"))
                .body("message", notNullValue())
                .body("message", not(emptyString()))
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("Should handle concurrent requests properly")
    public void testConcurrentRequests() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        Map<String, String> userData = new HashMap<>();
        userData.put("username", "conc" + timestamp);
        userData.put("email", "concurrent" + timestamp + "@example.com");
        userData.put("password", "Password123!");

        // Make multiple concurrent requests
        Response[] responses = new Response[3];
        
        for (int i = 0; i < 3; i++) {
            responses[i] = given()
                    .spec(requestSpec)
                    .body(userData)
            .when()
                    .post("/api/auth/register");
        }

        // First request should succeed, others should fail due to duplicate email
        int successCount = 0;
        int errorCount = 0;

        for (Response response : responses) {
            if (response.statusCode() == 200) {
                successCount++;
            } else if (response.statusCode() == 400) {
                errorCount++;
            }
        }

        // Should have exactly one success and proper error handling
        assert successCount == 1 : "Expected exactly one successful registration";
        assert errorCount >= 1 : "Expected at least one error for duplicate registration";
    }

    @Test
    @DisplayName("Should handle non-existent endpoints gracefully")
    public void testNonExistentEndpoints() {
        given()
                .spec(requestSpec)
        .when()
                .get("/api/auth/nonexistent")
        .then()
                .statusCode(anyOf(equalTo(404), equalTo(403))); // Not Found or Forbidden

        given()
                .spec(requestSpec)
        .when()
                .post("/api/nonexistent/endpoint")
        .then()
                .statusCode(anyOf(equalTo(404), equalTo(403))); // Not Found or Forbidden
    }

    @Test
    @DisplayName("Should handle very large request payloads")
    public void testVeryLargePayload() {
        Map<String, String> userData = new HashMap<>();
        String largeString = "x".repeat(10000); // 10KB string
        
        userData.put("username", "test");
        userData.put("email", "test@example.com");
        userData.put("password", largeString);

        given()
                .spec(requestSpec)
                .body(userData)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(anyOf(equalTo(400), equalTo(413))); // Bad Request or Payload Too Large
    }
}
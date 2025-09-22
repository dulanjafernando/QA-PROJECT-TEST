package com.qa.project.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for REST Assured API tests
 * Provides common configuration for all API test classes
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiTest {

    @LocalServerPort
    protected int port;

    protected static RequestSpecification requestSpec;

    @BeforeAll
    public static void setupRestAssured() {
        // Global REST Assured configuration
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @BeforeEach
    public void setupTest() {
        // Set the base URI and port for each test
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // Create a reusable request specification
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Access-Control-Allow-Origin", "*")
                .build();
    }

    /**
     * Get the base URL for API endpoints
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Get the full URL for a specific API endpoint
     */
    protected String getApiUrl(String endpoint) {
        return getBaseUrl() + "/api" + endpoint;
    }
}
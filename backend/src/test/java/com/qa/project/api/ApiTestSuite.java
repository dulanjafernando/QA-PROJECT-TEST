package com.qa.project.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * API Test Suite Runner
 * Runs all REST Assured API tests for authentication endpoints
 */
@Suite
@SelectClasses({
    UserRegistrationApiTest.class,
    UserLoginApiTest.class,
    ApiErrorHandlingTest.class
})
@DisplayName("Complete Authentication API Test Suite")
public class ApiTestSuite {
    // This class serves as a test suite runner
    // All API tests will be executed when this suite is run
}
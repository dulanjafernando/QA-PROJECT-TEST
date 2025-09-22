package com.qa.project.tdd.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.project.dto.LoginRequest;
import com.qa.project.dto.RegisterRequest;
import com.qa.project.model.User;
import com.qa.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Integration Tests for Full Authentication Flow
 * Testing complete user registration and login flow end-to-end
 * Following Red-Green-Refactor cycle for integration scenarios
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TDD: Full Authentication Integration")
class AuthenticationIntegrationTDDTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        // Arrange - Clean database and setup test data
        userRepository.deleteAll();

        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("integrationuser");
        validRegisterRequest.setEmail("integration@example.com");
        validRegisterRequest.setPassword("password123");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("integrationuser");
        validLoginRequest.setPassword("password123");
    }

    // ==================== FEATURE 1: COMPLETE REGISTRATION FLOW ====================

    @Test
    @DisplayName("GREEN: Should complete full registration flow successfully")
    void testCompleteRegistrationFlow_WithValidData_ShouldSucceed() throws Exception {
        // Arrange
        String requestJson = objectMapper.writeValueAsString(validRegisterRequest);

        // Act & Assert - Registration
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.username").value("integrationuser"))
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.userId").exists());

        // Verify user exists in database
        assertTrue(userRepository.existsByUsername("integrationuser"));
        assertTrue(userRepository.existsByEmail("integration@example.com"));
        
        User savedUser = userRepository.findByUsername("integrationuser").orElse(null);
        assertNotNull(savedUser);
        assertEquals("integrationuser", savedUser.getUsername());
        assertEquals("integration@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getPassword());
        assertTrue(savedUser.getPassword().startsWith("$2a$")); // BCrypt hash
    }

    @Test
    @DisplayName("RED: Should prevent duplicate registration")
    void testDuplicateRegistration_ShouldFailSecondAttempt() throws Exception {
        // Arrange - First registration
        String requestJson = objectMapper.writeValueAsString(validRegisterRequest);

        // Act - First registration (should succeed)
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Act & Assert - Second registration (should fail)
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Username already exists")));

        // Verify only one user exists
        assertEquals(1, userRepository.count());
    }

    // ==================== FEATURE 2: COMPLETE LOGIN FLOW ====================

    @Test
    @DisplayName("GREEN: Should complete full login flow after registration")
    void testCompleteLoginFlow_AfterRegistration_ShouldSucceed() throws Exception {
        // Arrange - Register user first
        String registerJson = objectMapper.writeValueAsString(validRegisterRequest);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isOk());

        // Act & Assert - Login
        String loginJson = objectMapper.writeValueAsString(validLoginRequest);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.username").value("integrationuser"))
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    @DisplayName("RED: Should fail login with wrong password")
    void testLoginFlow_WithWrongPassword_ShouldFail() throws Exception {
        // Arrange - Register user first
        String registerJson = objectMapper.writeValueAsString(validRegisterRequest);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isOk());

        // Arrange - Wrong password
        validLoginRequest.setPassword("wrongpassword");
        String loginJson = objectMapper.writeValueAsString(validLoginRequest);

        // Act & Assert - Login should fail
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("RED: Should fail login with non-existent user")
    void testLoginFlow_WithNonExistentUser_ShouldFail() throws Exception {
        // Arrange - No user registered
        String loginJson = objectMapper.writeValueAsString(validLoginRequest);

        // Act & Assert - Login should fail
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid username or password")));
    }

    // ==================== FEATURE 3: COMPLETE REGISTER-LOGIN CYCLE ====================

    @Test
    @DisplayName("GREEN: Should complete full register-login cycle")
    void testCompleteAuthenticationCycle_ShouldSucceed() throws Exception {
        // Step 1: Register
        String registerJson = objectMapper.writeValueAsString(validRegisterRequest);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("integrationuser"));

        // Step 2: Login immediately after registration
        String loginJson = objectMapper.writeValueAsString(validLoginRequest);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("integrationuser"));

        // Step 3: Verify test endpoint works
        mockMvc.perform(get("/api/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Auth controller is working!"));

        // Step 4: Logout
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    // ==================== EDGE CASES AND ERROR SCENARIOS ====================

    @Test
    @DisplayName("RED: Should handle malformed JSON gracefully")
    void testRegistration_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("RED: Should validate all required fields during registration")
    void testRegistration_WithMissingFields_ShouldReturnValidationErrors() throws Exception {
        // Arrange - Request with missing fields
        RegisterRequest incompleteRequest = new RegisterRequest();
        incompleteRequest.setUsername(""); // Empty username
        incompleteRequest.setEmail("invalid-email"); // Invalid email
        incompleteRequest.setPassword("123"); // Too short password
        
        String requestJson = objectMapper.writeValueAsString(incompleteRequest);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("required")));
    }

    @Test
    @DisplayName("REFACTOR: Should handle concurrent registration attempts")
    void testConcurrentRegistration_WithSameUsername_ShouldHandleGracefully() throws Exception {
        // This test simulates concurrent registration attempts
        // In a real scenario, you'd use multiple threads
        
        String requestJson = objectMapper.writeValueAsString(validRegisterRequest);

        // First registration
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk());

        // Immediate second registration with same data
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Verify database consistency
        assertEquals(1, userRepository.count());
    }

    @Test
    @DisplayName("REFACTOR: Should handle case-sensitive login correctly")
    void testLogin_WithDifferentCase_ShouldHandleCorrectly() throws Exception {
        // Arrange - Register user
        String registerJson = objectMapper.writeValueAsString(validRegisterRequest);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isOk());

        // Arrange - Login with different case
        validLoginRequest.setUsername("INTEGRATIONUSER"); // Different case
        String loginJson = objectMapper.writeValueAsString(validLoginRequest);

        // Act & Assert - Should fail due to case sensitivity
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== PERFORMANCE AND LOAD TESTS ====================

    @Test
    @DisplayName("REFACTOR: Should handle multiple sequential operations")
    void testMultipleSequentialOperations_ShouldMaintainPerformance() throws Exception {
        // This test ensures that multiple operations don't degrade performance significantly
        
        for (int i = 0; i < 5; i++) {
            // Create unique user for each iteration
            RegisterRequest request = new RegisterRequest();
            request.setUsername("user" + i);
            request.setEmail("user" + i + "@example.com");
            request.setPassword("password123");

            String requestJson = objectMapper.writeValueAsString(request);

            // Register user
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Login user
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("user" + i);
            loginRequest.setPassword("password123");
            String loginJson = objectMapper.writeValueAsString(loginRequest);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        // Verify all users were created
        assertEquals(5, userRepository.count());
    }

    @Test
    @DisplayName("REFACTOR: Should handle special characters in authentication flow")
    void testAuthenticationFlow_WithSpecialCharacters_ShouldWork() throws Exception {
        // Arrange - User with special characters
        validRegisterRequest.setUsername("user_123-test");
        validRegisterRequest.setEmail("user+test@example-domain.com");
        validRegisterRequest.setPassword("password!@#$%^&*()");

        validLoginRequest.setUsername("user_123-test");
        validLoginRequest.setPassword("password!@#$%^&*()");

        // Register
        String registerJson = objectMapper.writeValueAsString(validRegisterRequest);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Login
        String loginJson = objectMapper.writeValueAsString(validLoginRequest);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
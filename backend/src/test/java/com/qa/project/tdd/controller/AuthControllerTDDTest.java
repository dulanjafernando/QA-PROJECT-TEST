package com.qa.project.tdd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.project.controller.AuthController;
import com.qa.project.dto.AuthResponse;
import com.qa.project.dto.LoginRequest;
import com.qa.project.dto.RegisterRequest;
import com.qa.project.model.User;
import com.qa.project.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Test Suite for AuthController
 * Following Red-Green-Refactor methodology for REST API endpoints
 */
@WebMvcTest(AuthController.class)
@DisplayName("TDD: Authentication Controller API")
class AuthControllerTDDTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("password123");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    // ==================== FEATURE 1: USER REGISTRATION ENDPOINT ====================

    @Test
    @DisplayName("RED: POST /api/auth/register should register user successfully")
    void testRegisterEndpoint_WithValidData_ShouldReturnSuccess() throws Exception {
        // Arrange
        AuthResponse successResponse = new AuthResponse("Registration successful", 1L, "testuser", "test@example.com", true);
        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(successResponse);
        String requestJson = objectMapper.writeValueAsString(validRegisterRequest);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(userService).registerUser(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("RED: POST /api/auth/register should handle duplicate username")
    void testRegisterEndpoint_WithDuplicateUsername_ShouldReturnError() throws Exception {
        // Arrange
        AuthResponse errorResponse = new AuthResponse("Username already exists", false);
        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(errorResponse);
        String requestJson = objectMapper.writeValueAsString(validRegisterRequest);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    // ==================== FEATURE 2: USER LOGIN ENDPOINT ====================

    @Test
    @DisplayName("RED: POST /api/auth/login should authenticate user successfully")
    void testLoginEndpoint_WithValidCredentials_ShouldReturnSuccess() throws Exception {
        // Arrange
        AuthResponse successResponse = new AuthResponse("Login successful", 1L, "testuser", "test@example.com", true);
        when(userService.authenticateUser(any(LoginRequest.class))).thenReturn(successResponse);
        String requestJson = objectMapper.writeValueAsString(validLoginRequest);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(userService).authenticateUser(any(LoginRequest.class));
    }

    @Test
    @DisplayName("RED: POST /api/auth/login should handle invalid credentials")
    void testLoginEndpoint_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        AuthResponse errorResponse = new AuthResponse("Invalid username or password", false);
        when(userService.authenticateUser(any(LoginRequest.class))).thenReturn(errorResponse);
        
        validLoginRequest.setPassword("wrongpassword");
        String requestJson = objectMapper.writeValueAsString(validLoginRequest);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("RED: POST /api/auth/login should handle non-existent user")
    void testLoginEndpoint_WithNonExistentUser_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        AuthResponse errorResponse = new AuthResponse("Invalid username or password", false);
        when(userService.authenticateUser(any(LoginRequest.class))).thenReturn(errorResponse);
        
        validLoginRequest.setUsername("nonexistent");
        String requestJson = objectMapper.writeValueAsString(validLoginRequest);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid username or password")));
    }
}
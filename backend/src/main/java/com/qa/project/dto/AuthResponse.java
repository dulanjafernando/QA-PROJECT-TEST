package com.qa.project.dto;

public class AuthResponse {
    private String message;
    private Long userId;
    private String username;
    private String email;
    private String token;
    private boolean success;

    // Constructors
    public AuthResponse() {}

    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public AuthResponse(String message, Long userId, String username, String email, boolean success) {
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.success = success;
    }

    public AuthResponse(String message, Long userId, String username, String email, String token, boolean success) {
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.token = token;
        this.success = success;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
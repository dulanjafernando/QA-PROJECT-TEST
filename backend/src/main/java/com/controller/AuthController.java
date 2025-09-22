// backend/src/main/java/com/qa/project/controller/AuthController.java
package com.qa.project.controller;

import com.qa.project.dto.AuthResponse;
import com.qa.project.dto.LoginRequest;
import com.qa.project.dto.RegisterRequest;
import com.qa.project.service.UserService;
import com.qa.project.security.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:3000"})
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error -> {
                String message = error.getDefaultMessage();
                // Handle specific username validation cases
                if ("username".equals(error.getField())) {
                    String rejectedValue = String.valueOf(error.getRejectedValue());
                    if (rejectedValue != null && rejectedValue.length() < 3) {
                        message = "Username must be at least 3 characters";
                    } else if (rejectedValue != null && rejectedValue.length() > 20) {
                        message = "Username must be at most 20 characters";
                    }
                }
                errorMessage.append(message).append("; ");
            });
            
            // Log validation errors
            securityAuditService.logSecurityViolation("VALIDATION_ERROR", 
                errorMessage.toString(), ipAddress, userAgent);
            
            AuthResponse response = new AuthResponse(
                errorMessage.toString().trim(), 
                false
            );
            return ResponseEntity.badRequest().body(response);
        }
        
        AuthResponse response = userService.registerUser(registerRequest, ipAddress, userAgent);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(
            @Valid @RequestBody LoginRequest loginRequest,
            BindingResult bindingResult,
            HttpServletRequest request) {
        
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder();
            bindingResult.getFieldErrors().forEach(error -> 
                errorMessage.append(error.getDefaultMessage()).append("; ")
            );
            
            // Log validation errors
            securityAuditService.logSecurityViolation("VALIDATION_ERROR", 
                errorMessage.toString(), ipAddress, userAgent);
            
            AuthResponse response = new AuthResponse(
                errorMessage.toString().trim(), 
                false
            );
            return ResponseEntity.badRequest().body(response);
        }
        
        AuthResponse response = userService.authenticateUser(loginRequest, ipAddress, userAgent);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint(HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Log test endpoint access
        securityAuditService.logSuspiciousActivity("TEST_ENDPOINT_ACCESS", "anonymous", ipAddress, "Test endpoint accessed");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Auth controller is working!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("clientIp", ipAddress);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Log logout attempt
        securityAuditService.logTokenEvent("LOGOUT", "unknown", ipAddress, "User logged out");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract real client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
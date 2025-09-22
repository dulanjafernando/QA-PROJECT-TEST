// backend/src/main/java/com/qa/project/service/UserService.java
package com.qa.project.service;

import com.qa.project.dto.AuthResponse;
import com.qa.project.dto.LoginRequest;
import com.qa.project.dto.RegisterRequest;
import com.qa.project.model.User;
import com.qa.project.repository.UserRepository;
import com.qa.project.security.JwtUtil;
import com.qa.project.security.SecurityAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private SecurityAuditService securityAuditService;
    
    // Password strength regex - at least 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final String STRONG_PASSWORD_PATTERN = 
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
    
    private static final Pattern passwordPattern = Pattern.compile(STRONG_PASSWORD_PATTERN);
    
    public AuthResponse registerUser(RegisterRequest registerRequest) {
        return registerUser(registerRequest, "unknown", "unknown");
    }
    
    public AuthResponse registerUser(RegisterRequest registerRequest, String ipAddress, String userAgent) {
        try {
            // Input validation
            if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
                securityAuditService.logSecurityViolation("INVALID_INPUT", "Empty username in registration", ipAddress, userAgent);
                return new AuthResponse("Username cannot be empty", false);
            }
            
            if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
                securityAuditService.logSecurityViolation("INVALID_INPUT", "Empty email in registration", ipAddress, userAgent);
                return new AuthResponse("Email cannot be empty", false);
            }
            
            if (registerRequest.getPassword() == null || registerRequest.getPassword().trim().isEmpty()) {
                securityAuditService.logSecurityViolation("INVALID_INPUT", "Empty password in registration", ipAddress, userAgent);
                return new AuthResponse("Password cannot be empty", false);
            }
            
            // Check password strength
            if (!passwordPattern.matcher(registerRequest.getPassword()).matches()) {
                securityAuditService.logPasswordValidation(registerRequest.getUsername(), ipAddress, false);
                return new AuthResponse("Password must be at least 8 characters long and contain uppercase, lowercase, digit, and special character", false);
            }
            
            // Check if username already exists
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                securityAuditService.logSuspiciousActivity("DUPLICATE_USERNAME", registerRequest.getUsername(), ipAddress, "Attempted registration with existing username");
                return new AuthResponse("Username already exists", false);
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                securityAuditService.logSuspiciousActivity("DUPLICATE_EMAIL", registerRequest.getUsername(), ipAddress, "Attempted registration with existing email");
                return new AuthResponse("Email already exists", false);
            }
            
            // Create new user
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            
            // Hash the password with strong encryption
            String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
            user.setPassword(hashedPassword);
            
            // Save user
            User savedUser = userRepository.save(user);
            
            // Generate secure JWT token
            String accessToken = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getId());
            String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUsername(), savedUser.getId());
            
            // Log successful registration
            securityAuditService.logUserRegistration(savedUser.getUsername(), savedUser.getEmail(), ipAddress);
            securityAuditService.logTokenEvent("TOKEN_GENERATED", savedUser.getUsername(), ipAddress, "Registration successful");
            
            return new AuthResponse(
                "User registered successfully", 
                savedUser.getId(), 
                savedUser.getUsername(), 
                savedUser.getEmail(),
                accessToken,
                true
            );
            
        } catch (Exception e) {
            securityAuditService.logSecurityViolation("REGISTRATION_ERROR", e.getMessage(), ipAddress, userAgent);
            return new AuthResponse("Registration failed due to server error", false);
        }
    }
    
    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        return authenticateUser(loginRequest, "unknown", "unknown");
    }
    
    public AuthResponse authenticateUser(LoginRequest loginRequest, String ipAddress, String userAgent) {
        try {
            // Check if IP is locked due to failed attempts
            if (securityAuditService.isIpLocked(ipAddress)) {
                long remainingTime = securityAuditService.getRemainingLockoutTime(ipAddress);
                securityAuditService.logSecurityViolation("LOGIN_FROM_LOCKED_IP", 
                    "Attempted login from locked IP", ipAddress, userAgent);
                return new AuthResponse("Account temporarily locked. Try again in " + remainingTime + " minutes.", false);
            }
            
            // Input validation
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                securityAuditService.logFailedAuthentication("EMPTY_USERNAME", ipAddress, userAgent, "Empty username");
                return new AuthResponse("Username cannot be empty", false);
            }
            
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                securityAuditService.logFailedAuthentication(loginRequest.getUsername(), ipAddress, userAgent, "Empty password");
                return new AuthResponse("Password cannot be empty", false);
            }
            
            // Find user by username or email
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElse(userRepository.findByEmail(loginRequest.getUsername()).orElse(null));
            
            if (user == null) {
                securityAuditService.logFailedAuthentication(loginRequest.getUsername(), ipAddress, userAgent, "User not found");
                return new AuthResponse("Invalid credentials", false);
            }
            
            // Check password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                securityAuditService.logFailedAuthentication(user.getUsername(), ipAddress, userAgent, "Invalid password");
                return new AuthResponse("Invalid credentials", false);
            }

            // Generate secure JWT tokens
            String accessToken = jwtUtil.generateToken(user.getUsername(), user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());
            
            // Log successful authentication
            securityAuditService.logSuccessfulAuthentication(user.getUsername(), ipAddress, userAgent);
            securityAuditService.logTokenEvent("TOKEN_GENERATED", user.getUsername(), ipAddress, "Login successful");
            
            return new AuthResponse(
                "Login successful", 
                user.getId(), 
                user.getUsername(), 
                user.getEmail(),
                accessToken,
                true
            );
            
        } catch (Exception e) {
            securityAuditService.logSecurityViolation("LOGIN_ERROR", e.getMessage(), ipAddress, userAgent);
            return new AuthResponse("Authentication failed due to server error", false);
        }
    }
    
    // TDD-compatible methods
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User authenticateUser(String username, String password) {
        // Input validation
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        throw new RuntimeException("Invalid username or password");
    }

    public User registerUser(User user) {
        // Input validation
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Email format validation
        if (!user.getEmail().contains("@") || !user.getEmail().contains(".")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Username length validation
        if (user.getUsername().length() < 3 || user.getUsername().length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters");
        }
        
        // Password length validation
        if (user.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        // Check duplicates
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        
        // Hash password and save
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
}
// backend/src/main/java/com/qa/project/security/SecurityAuditService.java
package com.qa.project.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SecurityAuditService {
    
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    private static final Logger authLogger = LoggerFactory.getLogger("AUTH");
    
    // Track failed login attempts per IP
    private final Map<String, AtomicInteger> failedLoginAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailedAttempt = new ConcurrentHashMap<>();
    
    // Security thresholds
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    
    /**
     * Log successful authentication
     */
    public void logSuccessfulAuthentication(String username, String ipAddress, String userAgent) {
        authLogger.info("SUCCESSFUL_LOGIN - User: {}, IP: {}, UserAgent: {}, Time: {}", 
                username, ipAddress, userAgent, LocalDateTime.now());
        
        // Reset failed attempts on successful login
        failedLoginAttempts.remove(ipAddress);
        lastFailedAttempt.remove(ipAddress);
    }
    
    /**
     * Log failed authentication attempt
     */
    public void logFailedAuthentication(String username, String ipAddress, String userAgent, String reason) {
        authLogger.warn("FAILED_LOGIN - User: {}, IP: {}, UserAgent: {}, Reason: {}, Time: {}", 
                username, ipAddress, userAgent, reason, LocalDateTime.now());
        
        // Track failed attempts
        failedLoginAttempts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0)).incrementAndGet();
        lastFailedAttempt.put(ipAddress, LocalDateTime.now());
        
        int attempts = failedLoginAttempts.get(ipAddress).get();
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            securityLogger.error("SECURITY_ALERT - Multiple failed login attempts from IP: {}, Total attempts: {}, Time: {}", 
                    ipAddress, attempts, LocalDateTime.now());
        }
    }
    
    /**
     * Log user registration
     */
    public void logUserRegistration(String username, String email, String ipAddress) {
        authLogger.info("USER_REGISTRATION - User: {}, Email: {}, IP: {}, Time: {}", 
                username, email, ipAddress, LocalDateTime.now());
    }
    
    /**
     * Log security violations
     */
    public void logSecurityViolation(String violationType, String details, String ipAddress, String userAgent) {
        securityLogger.error("SECURITY_VIOLATION - Type: {}, Details: {}, IP: {}, UserAgent: {}, Time: {}", 
                violationType, details, ipAddress, userAgent, LocalDateTime.now());
    }
    
    /**
     * Log JWT token events
     */
    public void logTokenEvent(String eventType, String username, String ipAddress, String details) {
        securityLogger.info("TOKEN_EVENT - Type: {}, User: {}, IP: {}, Details: {}, Time: {}", 
                eventType, username, ipAddress, details, LocalDateTime.now());
    }
    
    /**
     * Check if IP is currently locked out due to failed attempts
     */
    public boolean isIpLocked(String ipAddress) {
        AtomicInteger attempts = failedLoginAttempts.get(ipAddress);
        if (attempts == null || attempts.get() < MAX_FAILED_ATTEMPTS) {
            return false;
        }
        
        LocalDateTime lastAttempt = lastFailedAttempt.get(ipAddress);
        if (lastAttempt == null) {
            return false;
        }
        
        LocalDateTime lockoutExpiry = lastAttempt.plusMinutes(LOCKOUT_DURATION_MINUTES);
        boolean isLocked = LocalDateTime.now().isBefore(lockoutExpiry);
        
        if (!isLocked) {
            // Lockout period has expired, reset counters
            failedLoginAttempts.remove(ipAddress);
            lastFailedAttempt.remove(ipAddress);
        }
        
        return isLocked;
    }
    
    /**
     * Get failed attempt count for IP
     */
    public int getFailedAttemptCount(String ipAddress) {
        AtomicInteger attempts = failedLoginAttempts.get(ipAddress);
        return attempts != null ? attempts.get() : 0;
    }
    
    /**
     * Get remaining lockout time in minutes
     */
    public long getRemainingLockoutTime(String ipAddress) {
        LocalDateTime lastAttempt = lastFailedAttempt.get(ipAddress);
        if (lastAttempt == null) {
            return 0;
        }
        
        LocalDateTime lockoutExpiry = lastAttempt.plusMinutes(LOCKOUT_DURATION_MINUTES);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(lockoutExpiry)) {
            return java.time.Duration.between(now, lockoutExpiry).toMinutes();
        }
        
        return 0;
    }
    
    /**
     * Log password validation events
     */
    public void logPasswordValidation(String username, String ipAddress, boolean isStrong) {
        if (!isStrong) {
            securityLogger.warn("WEAK_PASSWORD - User: {}, IP: {}, Time: {}", 
                    username, ipAddress, LocalDateTime.now());
        }
    }
    
    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String activityType, String username, String ipAddress, String details) {
        securityLogger.warn("SUSPICIOUS_ACTIVITY - Type: {}, User: {}, IP: {}, Details: {}, Time: {}", 
                activityType, username, ipAddress, details, LocalDateTime.now());
    }
}
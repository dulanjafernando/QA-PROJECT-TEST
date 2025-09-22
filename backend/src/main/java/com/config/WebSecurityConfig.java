// backend/src/main/java/com/qa/project/config/WebSecurityConfig.java
package com.qa.project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig implements WebMvcConfigurer {
    
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        // Use strength 12 for better security (default is 10)
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API (using JWT tokens)
            .csrf(csrf -> csrf.disable())
            
            // Configure session management for stateless authentication
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()  // Allow auth endpoints
                .requestMatchers("/api/auth/test").permitAll()   // Allow test endpoint (remove in production)
                .requestMatchers("/actuator/health").permitAll() // Allow health checks
                .anyRequest().authenticated()) // All other requests require authentication
            
            // Disable basic auth and form login for API
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())
            
            // Configure security headers (updated for current Spring Security version)
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny()) // Prevent clickjacking
                .contentTypeOptions(contentType -> contentType.disable()) // Prevent MIME type sniffing
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubDomains(true)
                    .preload(true))
                .cacheControl(cache -> cache.disable()) // Disable caching of sensitive content
            );
        
        return http.build();
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // Restrict origins to specific domains (not wildcard)
                .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://localhost:5174")
                // Limit allowed methods
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Restrict headers (avoid "*" in production)
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With")
                // Allow credentials for authentication
                .allowCredentials(true)
                // Set max age for preflight requests
                .maxAge(3600);
    }
}
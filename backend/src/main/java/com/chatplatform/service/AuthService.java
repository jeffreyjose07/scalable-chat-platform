package com.chatplatform.service;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
    public AuthService(UserService userService, 
                      JwtService jwtService,
                      AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }
    
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("Attempting login for email: {}", loginRequest.getEmail());
        
        // Find user by email for authentication
        User user = userService.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(), // Spring Security expects username
                        loginRequest.getPassword()
                )
        );
        
        // Generate JWT token
        String token = jwtService.generateToken(user);
        
        // Update user online status
        userService.updateUserOnlineStatus(user.getId(), true);
        
        logger.info("Login successful for user: {}", user.getUsername());
        return new AuthResponse(token, user);
    }
    
    public AuthResponse register(RegisterRequest registerRequest) {
        logger.info("Attempting registration for email: {}", registerRequest.getEmail());
        
        try {
            // Create new user
            User user = userService.createUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getDisplayName()
            );
            
            // Generate JWT token
            String token = jwtService.generateToken(user);
            
            // Set user as online
            userService.updateUserOnlineStatus(user.getId(), true);
            
            logger.info("Registration successful for user: {}", user.getUsername());
            return new AuthResponse(token, user);
            
        } catch (IllegalArgumentException e) {
            logger.error("Registration failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public User getUserFromToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("Token is required");
        }
        
        // Remove Bearer prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // Validate token
        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        
        // Extract username from token
        String username = jwtService.extractUsername(token);
        
        // Find user by username
        return userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public void logout(String token) {
        try {
            User user = getUserFromToken(token);
            userService.updateUserOnlineStatus(user.getId(), false);
            logger.info("Logout successful for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.warn("Logout failed: {}", e.getMessage());
        }
    }
}
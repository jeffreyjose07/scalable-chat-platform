package com.chatplatform.controller;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
import com.chatplatform.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            if (loginRequest.email() == null || loginRequest.email().trim().isEmpty()) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Email is required"));
            }
            
            if (loginRequest.password() == null || loginRequest.password().trim().isEmpty()) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Password is required"));
            }
            
            logger.info("Login attempt for email: {}", loginRequest.email());
            AuthResponse authResponse = authService.login(loginRequest);
            logger.info("Login successful for email: {}", loginRequest.email());
            return ResponseEntity.ok(authResponse);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed for email: {} - {}", loginRequest.email(), e.getMessage());
            return ResponseEntity.status(400)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (Exception e) {
            logger.error("Login failed for email: {}", loginRequest.email(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Login failed due to server error"));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (registerRequest.email() == null || registerRequest.email().trim().isEmpty()) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Email is required"));
            }
            
            if (registerRequest.username() == null || registerRequest.username().trim().isEmpty()) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Username is required"));
            }
            
            if (registerRequest.password() == null || registerRequest.password().length() < 6) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Password must be at least 6 characters"));
            }
            
            logger.info("Registration attempt for email: {}, username: {}", 
                registerRequest.email(), registerRequest.username());
            AuthResponse authResponse = authService.register(registerRequest);
            logger.info("Registration successful for email: {}", registerRequest.email());
            return ResponseEntity.ok(authResponse);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed for email: {} - {}", registerRequest.email(), e.getMessage());
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Registration failed for email: {}", registerRequest.email(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Registration failed due to server error"));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Get current user failed", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            authService.logout(authHeader);
            return ResponseEntity.ok(Map.of("message", "Logout successful"));
        } catch (Exception e) {
            logger.error("Logout failed", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Logout failed"));
        }
    }
}
package com.chatplatform.controller;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.MessageResponse;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
import com.chatplatform.service.AuthService;
import com.chatplatform.validator.AuthValidator;
import com.chatplatform.exception.ValidationException;
import com.chatplatform.exception.AuthenticationException;
import com.chatplatform.util.ResponseUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:3002"})
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthValidator authValidator;

    public AuthController(AuthService authService, AuthValidator authValidator) {
        this.authService = authService;
        this.authValidator = authValidator;
    }

    @PostMapping("/login")
    public ResponseEntity<MessageResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for email: {}", loginRequest.email());
            
            AuthResponse authResponse = authService.login(loginRequest);
            
            logger.info("Login successful for email: {}", loginRequest.email());
            return ResponseUtils.success("Login successful", authResponse);
            
        } catch (AuthenticationException e) {
            logger.warn("Login failed for email: {} - {}", loginRequest.email(), e.getMessage());
            return ResponseUtils.unauthorized(e.getMessage());
        } catch (Exception e) {
            logger.error("Login failed for email: {}", loginRequest.email(), e);
            return ResponseUtils.internalServerError("Login failed due to server error");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest, 
            BindingResult bindingResult) {
        try {
            // Validate request using dedicated validator
            authValidator.validateRegistrationRequest(registerRequest, bindingResult);
            
            logger.info("Registration attempt for email: {}", registerRequest.email());
            
            AuthResponse authResponse = authService.register(registerRequest);
            
            logger.info("Registration successful for email: {}", registerRequest.email());
            return ResponseUtils.created("User registered successfully", authResponse);
            
        } catch (ValidationException e) {
            logger.warn("Registration validation failed for email: {} - {}", 
                       registerRequest.email(), e.getMessage());
            return ResponseUtils.badRequest(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed for email: {} - {}", 
                       registerRequest.email(), e.getMessage());
            return ResponseUtils.conflict(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during registration for email: {}", 
                        registerRequest.email(), e);
            return ResponseUtils.internalServerError("Registration failed due to server error");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<MessageResponse<User>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            return ResponseUtils.success("User retrieved successfully", user);
        } catch (AuthenticationException e) {
            logger.warn("Get current user failed - {}", e.getMessage());
            return ResponseUtils.unauthorized("Invalid or expired token", (User) null);
        } catch (Exception e) {
            logger.error("Get current user failed", e);
            return ResponseUtils.internalServerError("Failed to retrieve user information", (User) null);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        try {
            authService.logout(authHeader);
            return ResponseUtils.success("Logout successful");
        } catch (Exception e) {
            logger.warn("Logout failed", e);
            return ResponseUtils.badRequest("Logout failed", (Void) null);
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse<Void>> forgotPassword(@RequestBody String email) {
        try {
            authService.sendPasswordResetEmail(email);
            return ResponseUtils.success("Password reset email sent if account exists");
        } catch (Exception e) {
            logger.error("Password reset failed for email: {}", email, e);
            // Always return success for security reasons (don't reveal if email exists)
            return ResponseUtils.success("Password reset email sent if account exists");
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        try {
            authService.resetPassword(token, newPassword);
            return ResponseUtils.success("Password reset successful");
        } catch (Exception e) {
            logger.warn("Password reset failed for token: {} - {}", token, e.getMessage());
            return ResponseUtils.badRequest("Invalid or expired reset token", (Void) null);
        }
    }
}

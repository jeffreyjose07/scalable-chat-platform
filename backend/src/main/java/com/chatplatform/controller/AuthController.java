package com.chatplatform.controller;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.ChangePasswordRequest;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.MessageResponse;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
import com.chatplatform.service.AuthService;
import com.chatplatform.service.UserService;
import com.chatplatform.validator.AuthValidator;
import com.chatplatform.exception.ValidationException;
import com.chatplatform.exception.AuthenticationException;
import com.chatplatform.util.ResponseUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuthValidator authValidator;
    private final UserService userService;

    public AuthController(AuthService authService, AuthValidator authValidator, UserService userService) {
        this.authService = authService;
        this.authValidator = authValidator;
        this.userService = userService;
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
    
    /**
     * Change user password with security validation
     */
    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            BindingResult bindingResult,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Password change requested by user: {}", userId);
        
        try {
            // Validate request
            if (bindingResult.hasErrors()) {
                throw new ValidationException("Invalid password change request: " + bindingResult.getFieldError().getDefaultMessage());
            }
            
            // Additional password strength validation
            if (!isPasswordStrong(request.getNewPassword())) {
                return ResponseUtils.badRequest("Password does not meet security requirements", (Void) null);
            }
            
            // Check if new password is different from current
            if (request.getCurrentPassword().equals(request.getNewPassword())) {
                return ResponseUtils.badRequest("New password must be different from current password", (Void) null);
            }
            
            // Change password via auth service
            authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            
            logger.info("Password changed successfully for user: {}", userId);
            return ResponseUtils.success("Password changed successfully");
            
        } catch (AuthenticationException e) {
            logger.warn("Password change failed - invalid current password for user: {}", userId);
            return ResponseUtils.badRequest("Current password is incorrect", (Void) null);
        } catch (ValidationException e) {
            logger.warn("Password change validation failed for user: {} - {}", userId, e.getMessage());
            return ResponseUtils.badRequest(e.getMessage(), (Void) null);
        } catch (Exception e) {
            logger.error("Unexpected error during password change for user: {}", userId, e);
            return ResponseUtils.internalServerError("Password change failed", (Void) null);
        }
    }
    
    /**
     * Validate password strength
     */
    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*(),.?\":{}|<>".indexOf(ch) >= 0);
        
        // Check for common weak passwords
        String lowerPassword = password.toLowerCase();
        String[] weakPasswords = {"password", "123456", "qwerty", "admin", "letmein", "welcome"};
        for (String weak : weakPasswords) {
            if (lowerPassword.contains(weak)) {
                return false;
            }
        }
        
        // Check for repeated characters
        if (password.matches("(.)\\1{2,}")) {
            return false;
        }
        
        // Require at least 3 of 4 character types
        int typesCount = 0;
        if (hasLower) typesCount++;
        if (hasUpper) typesCount++;
        if (hasDigit) typesCount++;
        if (hasSpecial) typesCount++;
        
        return typesCount >= 3;
    }
    
    /**
     * Update user profile information
     */
    @PutMapping("/profile")
    public ResponseEntity<MessageResponse<User>> updateProfile(
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {
        
        String userId = authentication.getName();
        logger.info("Profile update requested by user: {}", userId);
        
        try {
            // Find user by username (userId in our case is username)
            Optional<User> userOptional = userService.findByUsername(userId);
            if (userOptional.isEmpty()) {
                return ResponseUtils.notFound("User not found", (User) null);
            }
            
            User user = userOptional.get();
            boolean hasChanges = false;
            
            // Update displayName if provided
            if (updates.containsKey("displayName")) {
                String newDisplayName = (String) updates.get("displayName");
                if (newDisplayName != null && !newDisplayName.trim().isEmpty()) {
                    user.setDisplayName(newDisplayName.trim());
                    hasChanges = true;
                }
            }
            
            // Save changes if any
            if (hasChanges) {
                User updatedUser = userService.updateUser(user);
                logger.info("Profile updated successfully for user: {}", userId);
                return ResponseUtils.success("Profile updated successfully", updatedUser);
            } else {
                logger.info("No changes to update for user: {}", userId);
                return ResponseUtils.success("No changes to update", user);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during profile update for user: {}", userId, e);
            return ResponseUtils.internalServerError("Profile update failed", (User) null);
        }
    }
}

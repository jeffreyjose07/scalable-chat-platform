package com.chatplatform.controller;

import com.chatplatform.util.Constants;
import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.ChangePasswordRequest;
import com.chatplatform.dto.ForgotPasswordRequest;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.MessageResponse;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.dto.ResetPasswordRequest;
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
            return ResponseUtils.success(Constants.LOGIN_SUCCESSFUL, authResponse);
            
        } catch (AuthenticationException e) {
            logger.warn("Login failed for email: {} - {}", loginRequest.email(), e.getMessage());
            return ResponseUtils.unauthorized(e.getMessage());
        } catch (Exception e) {
            logger.error("Login failed for email: {}", loginRequest.email(), e);
            return ResponseUtils.internalServerError(Constants.LOGIN_FAILED_SERVER_ERROR);
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
            return ResponseUtils.created(Constants.USER_REGISTERED_SUCCESSFULLY, authResponse);
            
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
            return ResponseUtils.internalServerError(Constants.REGISTRATION_FAILED_SERVER_ERROR);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<MessageResponse<User>> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = authService.getUserFromToken(authHeader);
            return ResponseUtils.success(Constants.USER_RETRIEVED_SUCCESSFULLY, user);
        } catch (AuthenticationException e) {
            logger.warn("Get current user failed - {}", e.getMessage());
            return ResponseUtils.unauthorized(Constants.INVALID_OR_EXPIRED_TOKEN, (User) null);
        } catch (Exception e) {
            logger.error("Get current user failed", e);
            return ResponseUtils.internalServerError(Constants.FAILED_TO_RETRIEVE_USER_INFO, (User) null);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        try {
            authService.logout(authHeader);
            return ResponseUtils.success(Constants.LOGOUT_SUCCESSFUL);
        } catch (Exception e) {
            logger.warn("Logout failed", e);
            return ResponseUtils.badRequest(Constants.LOGOUT_FAILED, (Void) null);
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.sendPasswordResetEmail(request.email());
            return ResponseUtils.success(Constants.PASSWORD_RESET_EMAIL_SENT);
        } catch (Exception e) {
            logger.error("Password reset failed for email: {}", request.email(), e);
            // Always return success for security reasons (don't reveal if email exists)
            return ResponseUtils.success(Constants.PASSWORD_RESET_EMAIL_SENT);
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.token(), request.newPassword());
            return ResponseUtils.success(Constants.PASSWORD_RESET_SUCCESSFUL);
        } catch (Exception e) {
            logger.warn("Password reset failed: {}", e.getMessage());
            return ResponseUtils.badRequest(Constants.INVALID_OR_EXPIRED_RESET_TOKEN, (Void) null);
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
            ResponseEntity<MessageResponse<Void>> validationResult = validatePasswordChangeRequest(request, bindingResult);
            if (validationResult != null) {
                return validationResult;
            }
            
            authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            
            logger.info("Password changed successfully for user: {}", userId);
            return ResponseUtils.success(Constants.PASSWORD_CHANGED_SUCCESSFULLY);
            
        } catch (AuthenticationException e) {
            logger.warn("Password change failed - authentication error for user: {} - {}", userId, e.getMessage());
            return ResponseUtils.badRequest(e.getMessage(), (Void) null);
        } catch (ValidationException e) {
            logger.warn("Password change validation failed for user: {} - {}", userId, e.getMessage());
            return ResponseUtils.badRequest(e.getMessage(), (Void) null);
        } catch (IllegalArgumentException e) {
            logger.warn("Password change failed - invalid argument for user: {} - {}", userId, e.getMessage());
            return ResponseUtils.badRequest(e.getMessage(), (Void) null);
        } catch (Exception e) {
            logger.error("Unexpected error during password change for user: {}", userId, e);
            return ResponseUtils.internalServerError(Constants.UNEXPECTED_ERROR, (Void) null);
        }
    }
    
    /**
     * Validate password change request parameters
     */
    private ResponseEntity<MessageResponse<Void>> validatePasswordChangeRequest(
            ChangePasswordRequest request, BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid password change request");
            return ResponseUtils.badRequest(errorMessage, (Void) null);
        }
        
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            return ResponseUtils.badRequest(Constants.CURRENT_PASSWORD_REQUIRED, (Void) null);
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseUtils.badRequest(Constants.NEW_PASSWORD_REQUIRED, (Void) null);
        }
        
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            return ResponseUtils.badRequest(Constants.NEW_PASSWORD_MUST_BE_DIFFERENT, (Void) null);
        }
        
        String passwordValidationError = validatePasswordStrength(request.getNewPassword());
        if (passwordValidationError != null) {
            return ResponseUtils.badRequest(passwordValidationError, (Void) null);
        }
        
        return null; // Validation passed
    }
    
    /**
     * Validate password strength with detailed error messages
     */
    private String validatePasswordStrength(String password) {
        String basicValidationError = validateBasicPasswordRequirements(password);
        if (basicValidationError != null) {
            return basicValidationError;
        }
        
        String weakPasswordError = checkForWeakPasswords(password);
        if (weakPasswordError != null) {
            return weakPasswordError;
        }
        
        String characterTypeError = validateCharacterTypes(password);
        if (characterTypeError != null) {
            return characterTypeError;
        }
        
        return null; // Password is valid
    }
    
    /**
     * Validate basic password requirements (length, repeated characters)
     */
    private String validateBasicPasswordRequirements(String password) {
        if (password == null || password.length() < 8) {
            return Constants.PASSWORD_MIN_LENGTH;
        }
        
        if (password.matches("(.)\\1{2,}")) {
            return Constants.PASSWORD_NO_CONSECUTIVE_CHARS;
        }
        
        return null;
    }
    
    /**
     * Check for commonly used weak passwords
     */
    private String checkForWeakPasswords(String password) {
        String lowerPassword = password.toLowerCase();
        String[] weakPasswords = {"password", "123456", "qwerty", "admin", "letmein", "welcome"};
        
        for (String weak : weakPasswords) {
            if (lowerPassword.contains(weak)) {
                return Constants.PASSWORD_TOO_COMMON;
            }
        }
        
        return null;
    }
    
    /**
     * Validate password contains required character types
     */
    private String validateCharacterTypes(String password) {
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*(),.?\":{}|<>".indexOf(ch) >= 0);
        
        int typesCount = countCharacterTypes(hasLower, hasUpper, hasDigit, hasSpecial);
        
        if (typesCount < 3) {
            return buildCharacterTypeErrorMessage(hasLower, hasUpper, hasDigit, hasSpecial);
        }
        
        return null;
    }
    
    /**
     * Count the number of character types present in password
     */
    private int countCharacterTypes(boolean hasLower, boolean hasUpper, boolean hasDigit, boolean hasSpecial) {
        int count = 0;
        if (hasLower) count++;
        if (hasUpper) count++;
        if (hasDigit) count++;
        if (hasSpecial) count++;
        return count;
    }
    
    /**
     * Build error message for missing character types
     */
    private String buildCharacterTypeErrorMessage(boolean hasLower, boolean hasUpper, boolean hasDigit, boolean hasSpecial) {
        StringBuilder message = new StringBuilder(Constants.PASSWORD_REQUIREMENTS);
        
        if (!hasLower) message.append(Constants.LOWERCASE_LETTERS);
        if (!hasUpper) message.append(Constants.UPPERCASE_LETTERS);
        if (!hasDigit) message.append(Constants.NUMBERS);
        if (!hasSpecial) message.append(Constants.SPECIAL_CHARACTERS);
        
        String result = message.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
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
                return ResponseUtils.notFound(Constants.USER_NOT_FOUND, (User) null);
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
                return ResponseUtils.success(Constants.PROFILE_UPDATED_SUCCESSFULLY, updatedUser);
            } else {
                logger.info("No changes to update for user: {}", userId);
                return ResponseUtils.success(Constants.NO_CHANGES_TO_UPDATE, user);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during profile update for user: {}", userId, e);
            return ResponseUtils.internalServerError(Constants.PROFILE_UPDATE_FAILED, (User) null);
        }
    }
}

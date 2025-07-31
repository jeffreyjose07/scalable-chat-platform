package com.chatplatform.validator;

import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.exception.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Dedicated validator for authentication-related requests
 * Follows Single Responsibility Principle
 */
@Component
public class AuthValidator {

    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_DISPLAY_NAME_LENGTH = 50;

    /**
     * Validates registration request with comprehensive checks
     * @param request The registration request to validate
     * @param bindingResult Spring validation results
     * @throws ValidationException if validation fails
     */
    public void validateRegistrationRequest(RegisterRequest request, BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        
        // Collect Spring validation errors
        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
        }
        
        // Additional custom validations
        validateEmail(request.email(), errors);
        validateUsername(request.username(), errors);
        validatePassword(request.password(), errors);
        validateDisplayName(request.displayName(), errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = formatErrorMessage(errors);
            throw new ValidationException(errorMessage);
        }
    }

    private void validateEmail(String email, Map<String, String> errors) {
        if (email == null || email.trim().isEmpty()) {
            errors.put("email", "📧 Email address is required");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "📧 Please enter a valid email address (like user@example.com)");
        }
    }

    private void validateUsername(String username, Map<String, String> errors) {
        if (username == null || username.trim().isEmpty()) {
            errors.put("username", "👤 Username is required");
        } else if (!USERNAME_PATTERN.matcher(username).matches()) {
            errors.put("username", "👤 Username should be 3-20 characters using letters, numbers, and underscores only");
        }
    }

    private void validatePassword(String password, Map<String, String> errors) {
        if (password == null || password.trim().isEmpty()) {
            errors.put("password", "🔒 Password is required");
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.put("password", "🔒 Password needs at least " + MIN_PASSWORD_LENGTH + " characters to keep your account secure");
        } else if (isWeakPassword(password)) {
            errors.put("password", "🔐 Create a stronger password! Mix at least 3 of these: lowercase letters, UPPERCASE LETTERS, numbers (123), or symbols (!@#$)");
        }
    }

    private void validateDisplayName(String displayName, Map<String, String> errors) {
        if (displayName == null || displayName.trim().isEmpty()) {
            errors.put("displayName", "✨ Display name is required - this is how others will see you");
        } else if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            errors.put("displayName", "✨ Display name should be under " + MAX_DISPLAY_NAME_LENGTH + " characters to look great everywhere");
        }
    }

    private boolean isWeakPassword(String password) {
        // Enhanced password strength requirements
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialChar = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        // Require at least 3 of the 4 character types for strong password
        int strengthScore = 0;
        if (hasLowerCase) strengthScore++;
        if (hasUpperCase) strengthScore++;
        if (hasDigit) strengthScore++;
        if (hasSpecialChar) strengthScore++;
        
        return strengthScore < 3;
    }

    private String formatErrorMessage(Map<String, String> errors) {
        return errors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Validation failed");
    }
}
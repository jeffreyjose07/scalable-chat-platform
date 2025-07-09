package com.chatplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Registration request record using Java 17 features
 * Immutable data carrier with built-in validation and sanitization
 */
public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    String username,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,
    
    @NotBlank(message = "Display name is required")
    @Size(max = 50, message = "Display name must be less than 50 characters")
    String displayName
) {
    
    /**
     * Compact constructor for sanitization and validation
     */
    public RegisterRequest {
        username = sanitizeString(username);
        email = sanitizeEmail(email);
        password = Objects.requireNonNullElse(password, "").trim();
        displayName = sanitizeString(displayName);
    }
    
    /**
     * Factory method for creating sanitized registration request
     */
    public static RegisterRequest of(String username, String email, String password, String displayName) {
        return new RegisterRequest(username, email, password, displayName);
    }
    
    /**
     * Create a builder instance for fluent API construction
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Comprehensive validation using functional approach
     */
    public boolean isValid() {
        return isUsernameValid().and(isEmailValid()).and(isPasswordValid()).and(isDisplayNameValid()).test(this);
    }
    
    /**
     * Username validation predicate
     */
    private static Predicate<RegisterRequest> isUsernameValid() {
        return req -> req.username != null && 
                     !req.username.isBlank() && 
                     req.username.length() >= 3 && 
                     req.username.length() <= 20;
    }
    
    /**
     * Email validation predicate
     */
    private static Predicate<RegisterRequest> isEmailValid() {
        return req -> req.email != null && 
                     !req.email.isBlank() && 
                     req.email.contains("@");
    }
    
    /**
     * Password validation predicate
     */
    private static Predicate<RegisterRequest> isPasswordValid() {
        return req -> req.password != null && req.password.length() >= 6;
    }
    
    /**
     * Display name validation predicate
     */
    private static Predicate<RegisterRequest> isDisplayNameValid() {
        return req -> req.displayName != null && 
                     !req.displayName.isBlank() && 
                     req.displayName.length() <= 50;
    }
    
    /**
     * Sanitize string by trimming and handling nulls
     */
    private static String sanitizeString(String input) {
        return Objects.requireNonNullElse(input, "").trim();
    }
    
    /**
     * Sanitize email by trimming and converting to lowercase
     */
    private static String sanitizeEmail(String email) {
        return Objects.requireNonNullElse(email, "").trim().toLowerCase();
    }
    
    /**
     * Create a sanitized copy with masked password for logging
     */
    public RegisterRequest withMaskedPassword() {
        return new RegisterRequest(username, email, "****", displayName);
    }
    
    /**
     * Builder class for fluent API construction
     * Provides readable, maintainable way to construct RegisterRequest objects
     */
    public static class Builder {
        private String username;
        private String email;
        private String password;
        private String displayName;
        
        private Builder() {
            // Private constructor to enforce factory method usage
        }
        
        /**
         * Set the username (required)
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        /**
         * Set the email address (required)
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        /**
         * Set the password (required)
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        
        /**
         * Set the display name (required)
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        /**
         * Build the RegisterRequest instance
         * @return Immutable RegisterRequest with validation and sanitization
         */
        public RegisterRequest build() {
            return new RegisterRequest(username, email, password, displayName);
        }
        
        /**
         * Build with validation - throws exception if required fields are missing
         */
        public RegisterRequest buildWithValidation() {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username is required");
            }
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                throw new IllegalArgumentException("Display name is required");
            }
            return build();
        }
        
        /**
         * Create a builder from an existing RegisterRequest
         */
        public static Builder from(RegisterRequest request) {
            return new Builder()
                .username(request.username())
                .email(request.email())
                .password(request.password())
                .displayName(request.displayName());
        }
    }
}
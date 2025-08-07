package com.chatplatform.dto;

import com.chatplatform.model.User;
import java.time.Instant;
import java.util.Objects;

/**
 * Authentication response record using Java 17 features
 * Immutable data carrier for JWT authentication responses
 */
public record AuthResponse(
    String token,
    String type,
    UserInfo user,
    Instant issuedAt,
    boolean success
) {

    public static final String BEARER = "Bearer";

    /**
     * Default constructor with Bearer type
     */
    public AuthResponse(String token, User user) {
        this(token, BEARER, UserInfo.from(user), Instant.now(), true);
    }
    
    /**
     * Factory method for successful authentication
     */
    public static AuthResponse success(String token, User user) {
        return new AuthResponse(token, user);
    }
    
    /**
     * Create a builder instance for fluent API construction
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Factory method for failed authentication
     */
    public static AuthResponse failure() {
        return new AuthResponse(null, BEARER, null, Instant.now(), false);
    }
    
    /**
     * Factory method for failed authentication with message
     */
    public static AuthResponse failure(String message) {
        return new AuthResponse(null, BEARER, null, Instant.now(), false);
    }
    
    /**
     * Check if authentication was successful
     */
    public boolean isAuthenticated() {
        return success && token != null && !token.isBlank() && user != null;
    }
    
    /**
     * Nested record for user information
     * Prevents circular dependencies and provides clean API
     */
    public record UserInfo(
        String id,
        String username,
        String email,
        String displayName,
        Instant createdAt
    ) {
        
        /**
         * Factory method to create UserInfo from User entity
         */
        public static UserInfo from(User user) {
            return user != null ? new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getCreatedAt()
            ) : null;
        }
        
        /**
         * Create masked version for logging (without sensitive data)
         */
        public UserInfo masked() {
            return new UserInfo(
                id,
                username,
                maskEmail(email),
                displayName,
                createdAt
            );
        }
        
        /**
         * Mask email for privacy
         */
        private String maskEmail(String email) {
            if (email == null || email.length() < 3) {
                return "***";
            }
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                return email.charAt(0) + "***" + email.substring(atIndex);
            }
            return "***";
        }
    }
    
    /**
     * Compact constructor for validation
     */
    public AuthResponse {
        type = Objects.requireNonNullElse(type, BEARER);
        issuedAt = Objects.requireNonNullElse(issuedAt, Instant.now());
    }
    
    /**
     * Create a version safe for logging (without sensitive token)
     */
    public AuthResponse withoutSensitiveData() {
        return new AuthResponse(
            token != null ? "***" : null,
            type,
            user != null ? user.masked() : null,
            issuedAt,
            success
        );
    }
    
    /**
     * Builder class for fluent API construction
     * Provides readable, maintainable way to construct AuthResponse objects
     */
    public static class Builder {
        private String token;
        private String type = BEARER;
        private UserInfo user;
        private Instant issuedAt = Instant.now();
        private boolean success = true;
        
        private Builder() {
            // Private constructor to enforce factory method usage
        }
        
        /**
         * Set the JWT token
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }
        
        /**
         * Set the token type (defaults to Bearer)
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        /**
         * Set the user information from User entity
         */
        public Builder user(User user) {
            this.user = UserInfo.from(user);
            return this;
        }
        
        /**
         * Set the user information directly
         */
        public Builder userInfo(UserInfo userInfo) {
            this.user = userInfo;
            return this;
        }
        
        /**
         * Set the issued at timestamp (defaults to now)
         */
        public Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }
        
        /**
         * Set the success flag (defaults to true)
         */
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        /**
         * Convenience method to mark as successful
         */
        public Builder successful() {
            this.success = true;
            return this;
        }
        
        /**
         * Convenience method to mark as failed
         */
        public Builder failed() {
            this.success = false;
            this.token = null;
            this.user = null;
            return this;
        }
        
        /**
         * Build the AuthResponse instance
         */
        public AuthResponse build() {
            return new AuthResponse(token, type, user, issuedAt, success);
        }
        
        /**
         * Build with validation - ensures required fields for successful response
         */
        public AuthResponse buildWithValidation() {
            if (success && (token == null || token.trim().isEmpty())) {
                throw new IllegalArgumentException("Token is required for successful authentication");
            }
            if (success && user == null) {
                throw new IllegalArgumentException("User information is required for successful authentication");
            }
            return build();
        }
        
        /**
         * Create a builder from an existing AuthResponse
         */
        public static Builder from(AuthResponse response) {
            return new Builder()
                .token(response.token())
                .type(response.type())
                .userInfo(response.user())
                .issuedAt(response.issuedAt())
                .success(response.success());
        }
    }
}
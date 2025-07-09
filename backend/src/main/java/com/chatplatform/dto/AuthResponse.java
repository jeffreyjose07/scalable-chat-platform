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
     * Factory method for failed authentication
     */
    public static AuthResponse failure() {
        return new AuthResponse(null, "Bearer", null, Instant.now(), false);
    }
    
    /**
     * Factory method for failed authentication with message
     */
    public static AuthResponse failure(String message) {
        return new AuthResponse(null, "Bearer", null, Instant.now(), false);
    }
    
    /**
     * Check if authentication was successful
     */
    public boolean isAuthenticated() {
        return success && token != null && !token.isBlank();
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
}
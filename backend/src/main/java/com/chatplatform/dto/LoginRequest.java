package com.chatplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request record using Java 17 features
 * Immutable data carrier with built-in validation
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password
) {
    
    /**
     * Compact constructor for additional validation
     */
    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (password != null) {
            password = password.trim();
        }
    }
    
    /**
     * Factory method for creating sanitized login request
     */
    public static LoginRequest of(String email, String password) {
        return new LoginRequest(email, password);
    }
    
    /**
     * Validation helper using functional approach
     */
    public boolean isValid() {
        return email != null && !email.isBlank() && 
               password != null && password.length() >= 6;
    }
}
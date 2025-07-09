package com.chatplatform.exception;

/**
 * Custom exception for authentication-related errors
 * Provides clear separation between authentication and other exceptions
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
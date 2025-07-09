package com.chatplatform.exception;

/**
 * Custom exception for validation errors
 * Provides better error handling and separation of concerns
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.chatplatform.util;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for creating standardized HTTP responses
 * Follows DRY principle and provides consistent response formatting
 */
public class ResponseUtils {

    private static final AuthResponse EMPTY_AUTH_RESPONSE = 
        AuthResponse.builder()
            .failed()
            .build();

    // Success responses
    public static <T> ResponseEntity<MessageResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(MessageResponse.success(message, data));
    }

    public static ResponseEntity<MessageResponse<Void>> success(String message) {
        return ResponseEntity.ok(MessageResponse.success(message, null));
    }

    public static <T> ResponseEntity<MessageResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MessageResponse.success(message, data));
    }

    // Error responses
    public static ResponseEntity<MessageResponse<AuthResponse>> badRequest(String message) {
        return ResponseEntity.badRequest()
            .body(MessageResponse.error(message, EMPTY_AUTH_RESPONSE));
    }

    public static ResponseEntity<MessageResponse<AuthResponse>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse.error(message, EMPTY_AUTH_RESPONSE));
    }

    public static ResponseEntity<MessageResponse<AuthResponse>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(MessageResponse.error(message, EMPTY_AUTH_RESPONSE));
    }

    public static ResponseEntity<MessageResponse<AuthResponse>> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(MessageResponse.error(message, EMPTY_AUTH_RESPONSE));
    }

    // Generic error responses
    public static <T> ResponseEntity<MessageResponse<T>> unauthorized(String message, T data) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse.error(message, data));
    }

    public static <T> ResponseEntity<MessageResponse<T>> badRequest(String message, T data) {
        return ResponseEntity.badRequest()
            .body(MessageResponse.error(message, data));
    }

    public static <T> ResponseEntity<MessageResponse<T>> internalServerError(String message, T data) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(MessageResponse.error(message, data));
    }

    public static <T> ResponseEntity<MessageResponse<T>> conflict(String message, T data) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(MessageResponse.error(message, data));
    }

    public static <T> ResponseEntity<MessageResponse<T>> notFound(String message, T data) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(MessageResponse.error(message, data));
    }

    public static <T> ResponseEntity<MessageResponse<T>> forbidden(String message, T data) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(MessageResponse.error(message, data));
    }
}
package com.chatplatform.exception;

import com.chatplatform.dto.MessageResponse;
import com.chatplatform.util.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for consistent error responses
 * Centralizes error handling logic following DRY principle
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<MessageResponse<Object>> handleValidationException(ValidationException e) {
        logger.warn("Validation error: {}", e.getMessage());
        return ResponseUtils.badRequest(e.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponse<Object>> handleAuthenticationException(AuthenticationException e) {
        logger.warn("Authentication error: {}", e.getMessage());
        return ResponseUtils.unauthorized(e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        logger.warn("Validation error: {}", e.getMessage());
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Validation failed");
        return ResponseUtils.badRequest(errorMessage, null);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<MessageResponse<Object>> handleBindException(BindException e) {
        logger.warn("Binding error: {}", e.getMessage());
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Binding failed");
        return ResponseUtils.badRequest(errorMessage, null);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<MessageResponse<Object>> handleConversationNotFound(ConversationNotFoundException e) {
        logger.warn("Conversation not found: {}", e.getMessage());
        return ResponseUtils.notFound(e.getMessage(), Map.of("conversationId", e.getConversationId()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<MessageResponse<Object>> handleUserNotFound(UserNotFoundException e) {
        logger.warn("User not found: {}", e.getMessage());
        return ResponseUtils.notFound(e.getMessage(), Map.of(
            "userId", e.getUserId(),
            "searchCriteria", e.getSearchCriteria()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse<Object>> handleAccessDenied(AccessDeniedException e) {
        logger.warn("Access denied: {}", e.getMessage());
        Map<String, Object> details = Map.of(
            "resource", e.getResource() != null ? e.getResource() : "unknown",
            "action", e.getAction() != null ? e.getAction() : "unknown",
            "userId", e.getUserId() != null ? e.getUserId() : "unknown"
        );
        return ResponseUtils.forbidden(e.getMessage(), details);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        logger.error("Data integrity violation", e);
        String message = "The operation conflicts with existing data. Please check your input.";
        return ResponseUtils.conflict(message, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Illegal argument: {}", e.getMessage());
        return ResponseUtils.badRequest(e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse<Object>> handleGenericException(Exception e) {
        logger.error("Unexpected error", e);
        return ResponseUtils.internalServerError("An unexpected error occurred", null);
    }
}
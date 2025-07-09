package com.chatplatform.exception;

import com.chatplatform.dto.MessageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindException bindException;

    @Test
    void shouldHandleValidationException() {
        // Given
        ValidationException exception = new ValidationException("email: Email is required, password: Password is required");

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleValidationException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("email: Email is required, password: Password is required", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleAuthenticationException() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid credentials");

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleAuthenticationException(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Invalid credentials", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        FieldError fieldError1 = new FieldError("object", "email", "Invalid email format");
        FieldError fieldError2 = new FieldError("object", "password", "Password too short");
        
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(mock(org.springframework.validation.BindingResult.class));
        when(methodArgumentNotValidException.getBindingResult().getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleMethodArgumentNotValid(methodArgumentNotValidException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertTrue(response.getBody().message().contains("email: Invalid email format"));
        assertTrue(response.getBody().message().contains("password: Password too short"));
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleMethodArgumentNotValidExceptionWithSingleError() {
        // Given
        FieldError fieldError = new FieldError("object", "username", "Username is required");
        
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(mock(org.springframework.validation.BindingResult.class));
        when(methodArgumentNotValidException.getBindingResult().getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleMethodArgumentNotValid(methodArgumentNotValidException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("username: Username is required", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleMethodArgumentNotValidExceptionWithNoErrors() {
        // Given
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(mock(org.springframework.validation.BindingResult.class));
        when(methodArgumentNotValidException.getBindingResult().getFieldErrors()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleMethodArgumentNotValid(methodArgumentNotValidException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Validation failed", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleBindException() {
        // Given
        FieldError fieldError = new FieldError("object", "field", "Field is invalid");
        
        when(bindException.getBindingResult()).thenReturn(mock(org.springframework.validation.BindingResult.class));
        when(bindException.getBindingResult().getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleBindException(bindException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("field: Field is invalid", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleBindExceptionWithNoErrors() {
        // Given
        when(bindException.getBindingResult()).thenReturn(mock(org.springframework.validation.BindingResult.class));
        when(bindException.getBindingResult().getFieldErrors()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleBindException(bindException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Binding failed", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Username already exists");

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleIllegalArgumentException(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Username already exists", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error occurred");

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleNullPointerException() {
        // Given
        NullPointerException exception = new NullPointerException("Null pointer error");

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleValidationExceptionWithNullMessage() {
        // Given
        ValidationException exception = new ValidationException((String) null);

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleValidationException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertNull(response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleAuthenticationExceptionWithNullMessage() {
        // Given
        AuthenticationException exception = new AuthenticationException((String) null);

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleAuthenticationException(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertNull(response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleIllegalArgumentExceptionWithNullMessage() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException((String) null);

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleIllegalArgumentException(exception);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertNull(response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldHandleExceptionWithCause() {
        // Given
        Exception cause = new RuntimeException("Root cause");
        ValidationException exception = new ValidationException("Validation failed", cause);

        // When
        ResponseEntity<MessageResponse<Object>> response = 
            globalExceptionHandler.handleValidationException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Validation failed", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldCreateConsistentErrorResponseStructure() {
        // Given
        ValidationException validationException = new ValidationException("Validation error");
        AuthenticationException authenticationException = new AuthenticationException("Auth error");
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException("Illegal argument");
        RuntimeException runtimeException = new RuntimeException("Runtime error");

        // When
        ResponseEntity<MessageResponse<Object>> validationResponse = 
            globalExceptionHandler.handleValidationException(validationException);
        ResponseEntity<MessageResponse<Object>> authenticationResponse = 
            globalExceptionHandler.handleAuthenticationException(authenticationException);
        ResponseEntity<MessageResponse<Object>> illegalArgumentResponse = 
            globalExceptionHandler.handleIllegalArgumentException(illegalArgumentException);
        ResponseEntity<MessageResponse<Object>> runtimeResponse = 
            globalExceptionHandler.handleGenericException(runtimeException);

        // Then
        // All error responses should have consistent structure
        assertFalse(validationResponse.getBody().success());
        assertFalse(authenticationResponse.getBody().success());
        assertFalse(illegalArgumentResponse.getBody().success());
        assertFalse(runtimeResponse.getBody().success());

        // All should have null data
        assertNull(validationResponse.getBody().data());
        assertNull(authenticationResponse.getBody().data());
        assertNull(illegalArgumentResponse.getBody().data());
        assertNull(runtimeResponse.getBody().data());

        // All should have appropriate messages
        assertEquals("Validation error", validationResponse.getBody().message());
        assertEquals("Auth error", authenticationResponse.getBody().message());
        assertEquals("Illegal argument", illegalArgumentResponse.getBody().message());
        assertEquals("An unexpected error occurred", runtimeResponse.getBody().message());
    }
}
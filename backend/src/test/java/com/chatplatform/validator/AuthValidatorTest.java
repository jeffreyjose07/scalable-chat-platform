package com.chatplatform.validator;

import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthValidatorTest {

    @InjectMocks
    private AuthValidator authValidator;

    @Mock
    private BindingResult bindingResult;

    @Test
    void shouldValidateValidRegistrationRequest() {
        // Given
        RegisterRequest validRequest = RegisterRequest.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("password123")
            .displayName("Valid User")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> {
            authValidator.validateRegistrationRequest(validRequest, bindingResult);
        });

        verify(bindingResult).hasErrors();
    }

    @Test
    void shouldThrowValidationExceptionForInvalidEmail() {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("user")
            .email("invalid-email")
            .password("password123")
            .displayName("User Name")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        assertTrue(exception.getMessage().contains("Invalid email format"));
    }

    @Test
    void shouldThrowValidationExceptionForShortUsername() {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("ab")
            .email("valid@example.com")
            .password("password123")
            .displayName("User Name")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        assertTrue(exception.getMessage().contains("Username must be 3-20 characters"));
    }

    @Test
    void shouldThrowValidationExceptionForInvalidUsernameCharacters() {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("user@name")
            .email("valid@example.com")
            .password("password123")
            .displayName("User Name")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        assertTrue(exception.getMessage().contains("Username must be 3-20 characters"));
    }

    @Test
    void shouldThrowValidationExceptionForShortPassword() {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("123")
            .displayName("User Name")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        assertTrue(exception.getMessage().contains("Password must be at least 6 characters"));
    }

    @Test
    void shouldThrowValidationExceptionForWeakPassword() {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("onlyletters")
            .displayName("User Name")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        assertTrue(exception.getMessage().contains("Password must contain at least one letter and one number"));
    }

    @Test
    void shouldThrowValidationExceptionForLongDisplayName() {
        // Given
        String longDisplayName = "A".repeat(51); // 51 characters
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("password123")
            .displayName(longDisplayName)
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        assertTrue(exception.getMessage().contains("Display name must be less than 50 characters"));
    }

    @Test
    void shouldThrowValidationExceptionForEmptyFields() {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("")
            .email("")
            .password("")
            .displayName("")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        String message = exception.getMessage();
        assertTrue(message.contains("Email is required"));
        assertTrue(message.contains("Username is required"));
        assertTrue(message.contains("Password is required"));
        assertTrue(message.contains("Display name is required"));
    }

    @Test
    void shouldIncludeBindingResultErrors() {
        // Given
        RegisterRequest request = RegisterRequest.of(
            "validuser", 
            "valid@example.com", 
            "password123", 
            "Valid User"
        );
        
        FieldError fieldError = new FieldError("registerRequest", "username", "Username already exists");
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError));

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(request, bindingResult);
        });

        assertTrue(exception.getMessage().contains("Username already exists"));
    }

    @Test
    void shouldCombineMultipleValidationErrors() {
        // Given
        RegisterRequest invalidRequest = RegisterRequest.builder()
            .username("ab")
            .email("invalid-email")
            .password("123")
            .displayName("")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            authValidator.validateRegistrationRequest(invalidRequest, bindingResult);
        });

        String message = exception.getMessage();
        assertTrue(message.contains("email"));
        assertTrue(message.contains("username"));
        assertTrue(message.contains("password"));
        assertTrue(message.contains("displayName"));
    }

    @Test
    void shouldAcceptValidPasswordWithLettersAndNumbers() {
        // Given
        RegisterRequest validRequest = RegisterRequest.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("abc123")
            .displayName("Valid User")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> {
            authValidator.validateRegistrationRequest(validRequest, bindingResult);
        });
    }

    @Test
    void shouldAcceptUsernameWithUnderscores() {
        // Given
        RegisterRequest validRequest = RegisterRequest.builder()
            .username("valid_user_123")
            .email("valid@example.com")
            .password("password123")
            .displayName("Valid User")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> {
            authValidator.validateRegistrationRequest(validRequest, bindingResult);
        });
    }

    @Test
    void shouldAcceptMaxLengthDisplayName() {
        // Given
        String maxDisplayName = "A".repeat(50); // Exactly 50 characters
        RegisterRequest validRequest = RegisterRequest.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("password123")
            .displayName(maxDisplayName)
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> {
            authValidator.validateRegistrationRequest(validRequest, bindingResult);
        });
    }

    @Test
    void shouldAcceptMaxLengthUsername() {
        // Given
        String maxUsername = "A".repeat(20); // Exactly 20 characters
        RegisterRequest validRequest = RegisterRequest.builder()
            .username(maxUsername)
            .email("valid@example.com")
            .password("password123")
            .displayName("Valid User")
            .build();
        when(bindingResult.hasErrors()).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> {
            authValidator.validateRegistrationRequest(validRequest, bindingResult);
        });
    }
}
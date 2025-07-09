package com.chatplatform.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestBuilderTest {

    @Test
    void shouldBuildValidRegisterRequestWithAllFields() {
        // When
        RegisterRequest request = RegisterRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .displayName("Test User")
            .build();

        // Then
        assertEquals("testuser", request.username());
        assertEquals("test@example.com", request.email());
        assertEquals("password123", request.password());
        assertEquals("Test User", request.displayName());
        assertTrue(request.isValid());
    }

    @Test
    void shouldBuildWithFluentAPI() {
        // When
        RegisterRequest request = RegisterRequest.builder()
            .username("john_doe")
            .email("JOHN@EXAMPLE.COM")  // Should be sanitized to lowercase
            .password("  secure123  ")   // Should be trimmed
            .displayName("  John Doe  ") // Should be trimmed
            .build();

        // Then
        assertEquals("john_doe", request.username());
        assertEquals("john@example.com", request.email()); // Sanitized
        assertEquals("secure123", request.password()); // Trimmed
        assertEquals("John Doe", request.displayName()); // Trimmed
    }

    @Test
    void shouldBuildWithPartialFields() {
        // When
        RegisterRequest request = RegisterRequest.builder()
            .username("user")
            .email("user@test.com")
            .password("pass123")
            .displayName("User")
            .build();

        // Then
        assertNotNull(request);
        assertEquals("user", request.username());
        assertEquals("user@test.com", request.email());
        assertEquals("pass123", request.password());
        assertEquals("User", request.displayName());
    }

    @Test
    void shouldBuildWithValidationSuccess() {
        // When
        RegisterRequest request = RegisterRequest.builder()
            .username("validuser")
            .email("valid@example.com")
            .password("password123")
            .displayName("Valid User")
            .buildWithValidation();

        // Then
        assertNotNull(request);
        assertTrue(request.isValid());
    }

    @Test
    void shouldThrowExceptionWhenBuildingWithValidationAndMissingUsername() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .displayName("Test User")
                .buildWithValidation();
        });

        assertEquals("Username is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenBuildingWithValidationAndMissingEmail() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            RegisterRequest.builder()
                .username("testuser")
                .password("password123")
                .displayName("Test User")
                .buildWithValidation();
        });

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenBuildingWithValidationAndMissingPassword() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .buildWithValidation();
        });

        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenBuildingWithValidationAndMissingDisplayName() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .buildWithValidation();
        });

        assertEquals("Display name is required", exception.getMessage());
    }

    @Test
    void shouldCreateBuilderFromExistingRequest() {
        // Given
        RegisterRequest originalRequest = RegisterRequest.builder()
            .username("original")
            .email("original@test.com")
            .password("original123")
            .displayName("Original User")
            .build();

        // When
        RegisterRequest modifiedRequest = RegisterRequest.Builder.from(originalRequest)
            .username("modified")
            .email("modified@test.com")
            .build();

        // Then
        assertEquals("modified", modifiedRequest.username());
        assertEquals("modified@test.com", modifiedRequest.email());
        assertEquals("original123", modifiedRequest.password()); // Unchanged
        assertEquals("Original User", modifiedRequest.displayName()); // Unchanged
    }

    @Test
    void shouldAllowMethodChaining() {
        // When
        RegisterRequest request = RegisterRequest.builder()
            .username("chaintest")
            .email("chain@test.com")
            .password("chain123")
            .displayName("Chain Test")
            .build();

        // Then
        assertNotNull(request);
        assertEquals("chaintest", request.username());
        assertEquals("chain@test.com", request.email());
        assertEquals("chain123", request.password());
        assertEquals("Chain Test", request.displayName());
    }

    @Test
    void shouldCreateMaskedVersionFromBuilder() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("secretpassword")
            .displayName("Test User")
            .build();

        // When
        RegisterRequest maskedRequest = request.withMaskedPassword();

        // Then
        assertEquals("testuser", maskedRequest.username());
        assertEquals("test@example.com", maskedRequest.email());
        assertEquals("****", maskedRequest.password());
        assertEquals("Test User", maskedRequest.displayName());
    }

    @Test
    void shouldHandleNullValuesGracefully() {
        // When
        RegisterRequest request = RegisterRequest.builder()
            .username(null)
            .email(null)
            .password(null)
            .displayName(null)
            .build();

        // Then
        assertNotNull(request);
        assertEquals("", request.username());  // Sanitized to empty string
        assertEquals("", request.email());    // Sanitized to empty string
        assertEquals("", request.password()); // Sanitized to empty string
        assertEquals("", request.displayName()); // Sanitized to empty string
        assertFalse(request.isValid()); // Should be invalid
    }

    @Test
    void shouldCompareBuilderCreatedObjectsWithFactoryMethod() {
        // Given
        RegisterRequest builderRequest = RegisterRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .displayName("Test User")
            .build();

        RegisterRequest factoryRequest = RegisterRequest.of("testuser", "test@example.com", "password123", "Test User");

        // Then
        assertEquals(builderRequest.username(), factoryRequest.username());
        assertEquals(builderRequest.email(), factoryRequest.email());
        assertEquals(builderRequest.password(), factoryRequest.password());
        assertEquals(builderRequest.displayName(), factoryRequest.displayName());
        assertEquals(builderRequest.isValid(), factoryRequest.isValid());
    }
}
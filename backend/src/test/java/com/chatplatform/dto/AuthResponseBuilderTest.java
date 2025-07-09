package com.chatplatform.dto;

import com.chatplatform.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AuthResponseBuilderTest {

    private User testUser;
    private Instant testTime;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(Instant.now());
        
        testTime = Instant.parse("2023-01-01T00:00:00Z");
    }

    @Test
    void shouldBuildSuccessfulAuthResponseWithAllFields() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("jwt-token-123")
            .type("Bearer")
            .user(testUser)
            .issuedAt(testTime)
            .success(true)
            .build();

        // Then
        assertEquals("jwt-token-123", response.token());
        assertEquals("Bearer", response.type());
        assertNotNull(response.user());
        assertEquals("testuser", response.user().username());
        assertEquals("test@example.com", response.user().email());
        assertEquals(testTime, response.issuedAt());
        assertTrue(response.success());
        assertTrue(response.isAuthenticated());
    }

    @Test
    void shouldBuildWithDefaultValues() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("jwt-token")
            .user(testUser)
            .build();

        // Then
        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.type()); // Default
        assertNotNull(response.user());
        assertNotNull(response.issuedAt()); // Default to now
        assertTrue(response.success()); // Default
        assertTrue(response.isAuthenticated());
    }

    @Test
    void shouldBuildFailedAuthResponse() {
        // When
        AuthResponse response = AuthResponse.builder()
            .failed()
            .build();

        // Then
        assertNull(response.token());
        assertEquals("Bearer", response.type());
        assertNull(response.user());
        assertFalse(response.success());
        assertFalse(response.isAuthenticated());
    }

    @Test
    void shouldBuildSuccessfulAuthResponseWithConvenienceMethod() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("jwt-token")
            .user(testUser)
            .successful()
            .build();

        // Then
        assertEquals("jwt-token", response.token());
        assertTrue(response.success());
        assertTrue(response.isAuthenticated());
    }

    @Test
    void shouldBuildWithCustomType() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("custom-token")
            .type("Custom")
            .user(testUser)
            .build();

        // Then
        assertEquals("custom-token", response.token());
        assertEquals("Custom", response.type());
        assertTrue(response.success());
    }

    @Test
    void shouldBuildWithUserInfo() {
        // Given
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            "123", "john", "john@test.com", "John Doe", testTime
        );

        // When
        AuthResponse response = AuthResponse.builder()
            .token("jwt-token")
            .userInfo(userInfo)
            .build();

        // Then
        assertEquals("jwt-token", response.token());
        assertEquals(userInfo, response.user());
        assertTrue(response.success());
    }

    @Test
    void shouldBuildWithValidationSuccess() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("valid-token")
            .user(testUser)
            .buildWithValidation();

        // Then
        assertNotNull(response);
        assertTrue(response.isAuthenticated());
    }

    @Test
    void shouldThrowExceptionWhenBuildingWithValidationAndMissingToken() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            AuthResponse.builder()
                .user(testUser)
                .success(true)
                .buildWithValidation();
        });

        assertEquals("Token is required for successful authentication", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenBuildingWithValidationAndMissingUser() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            AuthResponse.builder()
                .token("jwt-token")
                .success(true)
                .buildWithValidation();
        });

        assertEquals("User information is required for successful authentication", exception.getMessage());
    }

    @Test
    void shouldAllowValidationForFailedResponse() {
        // When
        AuthResponse response = AuthResponse.builder()
            .failed()
            .buildWithValidation();

        // Then
        assertNotNull(response);
        assertFalse(response.success());
        assertFalse(response.isAuthenticated());
    }

    @Test
    void shouldCreateBuilderFromExistingResponse() {
        // Given
        AuthResponse originalResponse = AuthResponse.builder()
            .token("original-token")
            .user(testUser)
            .issuedAt(testTime)
            .build();

        // When
        AuthResponse modifiedResponse = AuthResponse.Builder.from(originalResponse)
            .token("modified-token")
            .build();

        // Then
        assertEquals("modified-token", modifiedResponse.token());
        assertEquals("Bearer", modifiedResponse.type()); // Unchanged
        assertEquals(originalResponse.user(), modifiedResponse.user()); // Unchanged
        assertEquals(testTime, modifiedResponse.issuedAt()); // Unchanged
        assertTrue(modifiedResponse.success()); // Unchanged
    }

    @Test
    void shouldAllowMethodChaining() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("chain-token")
            .type("Bearer")
            .user(testUser)
            .issuedAt(testTime)
            .successful()
            .build();

        // Then
        assertNotNull(response);
        assertEquals("chain-token", response.token());
        assertEquals("Bearer", response.type());
        assertEquals(testTime, response.issuedAt());
        assertTrue(response.success());
    }

    @Test
    void shouldCreateSensitiveDataMaskedVersion() {
        // Given
        AuthResponse response = AuthResponse.builder()
            .token("sensitive-token")
            .user(testUser)
            .build();

        // When
        AuthResponse maskedResponse = response.withoutSensitiveData();

        // Then
        assertEquals("***", maskedResponse.token());
        assertNotNull(maskedResponse.user());
        assertNotEquals(testUser.getEmail(), maskedResponse.user().email()); // Email should be masked
    }

    @Test
    void shouldCompareBuilderCreatedObjectsWithFactoryMethod() {
        // Given
        AuthResponse builderResponse = AuthResponse.builder()
            .token("test-token")
            .user(testUser)
            .build();

        AuthResponse factoryResponse = AuthResponse.success("test-token", testUser);

        // Then
        assertEquals(builderResponse.token(), factoryResponse.token());
        assertEquals(builderResponse.type(), factoryResponse.type());
        assertEquals(builderResponse.user().username(), factoryResponse.user().username());
        assertEquals(builderResponse.user().email(), factoryResponse.user().email());
        assertEquals(builderResponse.success(), factoryResponse.success());
        assertTrue(builderResponse.isAuthenticated());
        assertTrue(factoryResponse.isAuthenticated());
    }

    @Test
    void shouldHandleNullTokenGracefully() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token(null)
            .user(testUser)
            .build();

        // Then
        assertNull(response.token());
        assertFalse(response.isAuthenticated()); // Should be false due to null token
        assertTrue(response.success()); // success flag can still be true
    }

    @Test
    void shouldHandleEmptyTokenGracefully() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("")
            .user(testUser)
            .build();

        // Then
        assertEquals("", response.token());
        assertFalse(response.isAuthenticated()); // Should be false due to empty token
        assertTrue(response.success()); // success flag can still be true
    }

    @Test
    void shouldBuildResponseWithoutUser() {
        // When
        AuthResponse response = AuthResponse.builder()
            .token("token-without-user")
            .build();

        // Then
        assertEquals("token-without-user", response.token());
        assertNull(response.user());
        assertTrue(response.success());
        assertFalse(response.isAuthenticated()); // Should be false due to missing user
    }
}
package com.chatplatform.util;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.MessageResponse;
import com.chatplatform.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ResponseUtilsTest {

    private User testUser;
    private AuthResponse testAuthResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(Instant.now());
        
        testAuthResponse = AuthResponse.builder()
            .token("test-token")
            .user(testUser)
            .build();
    }

    @Test
    void shouldCreateSuccessResponseWithData() {
        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = 
            ResponseUtils.success("Operation successful", testAuthResponse);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Operation successful", response.getBody().message());
        assertEquals(testAuthResponse, response.getBody().data());
    }

    @Test
    void shouldCreateSuccessResponseWithoutData() {
        // When
        ResponseEntity<MessageResponse<Void>> response = 
            ResponseUtils.success("Operation successful");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Operation successful", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldCreateCreatedResponse() {
        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = 
            ResponseUtils.created("Resource created", testAuthResponse);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Resource created", response.getBody().message());
        assertEquals(testAuthResponse, response.getBody().data());
    }

    @Test
    void shouldCreateBadRequestResponse() {
        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = 
            ResponseUtils.badRequest("Bad request error");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Bad request error", response.getBody().message());
        assertNotNull(response.getBody().data());
        assertFalse(response.getBody().data().success());
    }

    @Test
    void shouldCreateUnauthorizedResponse() {
        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = 
            ResponseUtils.unauthorized("Unauthorized access");

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Unauthorized access", response.getBody().message());
        assertNotNull(response.getBody().data());
        assertFalse(response.getBody().data().success());
    }

    @Test
    void shouldCreateConflictResponse() {
        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = 
            ResponseUtils.conflict("Resource conflict");

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Resource conflict", response.getBody().message());
        assertNotNull(response.getBody().data());
        assertFalse(response.getBody().data().success());
    }

    @Test
    void shouldCreateInternalServerErrorResponse() {
        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = 
            ResponseUtils.internalServerError("Internal server error");

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Internal server error", response.getBody().message());
        assertNotNull(response.getBody().data());
        assertFalse(response.getBody().data().success());
    }

    @Test
    void shouldCreateGenericUnauthorizedResponse() {
        // When
        ResponseEntity<MessageResponse<String>> response = 
            ResponseUtils.unauthorized("Unauthorized", "test-data");

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Unauthorized", response.getBody().message());
        assertEquals("test-data", response.getBody().data());
    }

    @Test
    void shouldCreateGenericBadRequestResponse() {
        // When
        ResponseEntity<MessageResponse<String>> response = 
            ResponseUtils.badRequest("Bad request", "error-data");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Bad request", response.getBody().message());
        assertEquals("error-data", response.getBody().data());
    }

    @Test
    void shouldCreateGenericInternalServerErrorResponse() {
        // When
        ResponseEntity<MessageResponse<String>> response = 
            ResponseUtils.internalServerError("Server error", "error-data");

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Server error", response.getBody().message());
        assertEquals("error-data", response.getBody().data());
    }

    @Test
    void shouldCreateGenericConflictResponse() {
        // When
        ResponseEntity<MessageResponse<String>> response = 
            ResponseUtils.conflict("Conflict error", "conflict-data");

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Conflict error", response.getBody().message());
        assertEquals("conflict-data", response.getBody().data());
    }

    @Test
    void shouldHandleNullMessagesGracefully() {
        // When
        ResponseEntity<MessageResponse<String>> response = 
            ResponseUtils.success(null, "test-data");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertNull(response.getBody().message());
        assertEquals("test-data", response.getBody().data());
    }

    @Test
    void shouldHandleNullDataGracefully() {
        // When
        ResponseEntity<MessageResponse<String>> response = 
            ResponseUtils.success("Success message", null);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Success message", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void shouldCreateConsistentErrorResponseStructure() {
        // When
        ResponseEntity<MessageResponse<AuthResponse>> badRequest = ResponseUtils.badRequest("Bad request");
        ResponseEntity<MessageResponse<AuthResponse>> unauthorized = ResponseUtils.unauthorized("Unauthorized");
        ResponseEntity<MessageResponse<AuthResponse>> conflict = ResponseUtils.conflict("Conflict");
        ResponseEntity<MessageResponse<AuthResponse>> serverError = ResponseUtils.internalServerError("Server error");

        // Then
        // All error responses should have the same structure
        assertFalse(badRequest.getBody().success());
        assertFalse(unauthorized.getBody().success());
        assertFalse(conflict.getBody().success());
        assertFalse(serverError.getBody().success());

        // All should have empty AuthResponse data
        assertNotNull(badRequest.getBody().data());
        assertNotNull(unauthorized.getBody().data());
        assertNotNull(conflict.getBody().data());
        assertNotNull(serverError.getBody().data());

        assertFalse(badRequest.getBody().data().success());
        assertFalse(unauthorized.getBody().data().success());
        assertFalse(conflict.getBody().data().success());
        assertFalse(serverError.getBody().data().success());
    }
}
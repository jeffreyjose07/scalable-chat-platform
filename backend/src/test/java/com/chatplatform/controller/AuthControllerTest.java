package com.chatplatform.controller;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.dto.MessageResponse;
import com.chatplatform.model.User;
import com.chatplatform.service.AuthService;
import com.chatplatform.validator.AuthValidator;
import com.chatplatform.exception.ValidationException;
import com.chatplatform.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthValidator authValidator;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private AuthResponse authResponse;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(Instant.now());

        authResponse = AuthResponse.builder()
            .token("test-jwt-token")
            .user(testUser)
            .build();

        loginRequest = new LoginRequest("test@example.com", "password123");

        registerRequest = RegisterRequest.builder()
            .username("newuser")
            .email("new@example.com")
            .password("password123")
            .displayName("New User")
            .build();
    }

    @Test
    void shouldLoginSuccessfully() {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = authController.login(loginRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Login successful", response.getBody().message());
        assertEquals(authResponse, response.getBody().data());
        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginFails() {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("Invalid email or password"));

        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = authController.login(loginRequest);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Invalid email or password", response.getBody().message());
        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = authController.login(loginRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Login failed due to server error", response.getBody().message());
        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void shouldRegisterSuccessfully() {
        // Given
        doNothing().when(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = authController.register(registerRequest, bindingResult);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("User registered successfully", response.getBody().message());
        assertEquals(authResponse, response.getBody().data());
        verify(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));
        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() {
        // Given
        doThrow(new ValidationException("email: Email is required, password: Password is required"))
                .when(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));

        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = authController.register(registerRequest, bindingResult);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("email: Email is required, password: Password is required", response.getBody().message());
        verify(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));
        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturnConflictWhenRegistrationFails() {
        // Given
        doNothing().when(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = authController.register(registerRequest, bindingResult);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Username already exists", response.getBody().message());
        verify(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));
        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnInternalServerErrorWhenRegistrationThrowsUnexpectedException() {
        // Given
        doNothing().when(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        ResponseEntity<MessageResponse<AuthResponse>> response = authController.register(registerRequest, bindingResult);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Registration failed due to server error", response.getBody().message());
        verify(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));
        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldGetCurrentUserSuccessfully() {
        // Given
        when(authService.getUserFromToken(anyString())).thenReturn(testUser);

        // When
        ResponseEntity<MessageResponse<User>> response = authController.getCurrentUser("Bearer test-jwt-token");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("User retrieved successfully", response.getBody().message());
        assertEquals(testUser, response.getBody().data());
        verify(authService).getUserFromToken("Bearer test-jwt-token");
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid() {
        // Given
        when(authService.getUserFromToken(anyString()))
                .thenThrow(new AuthenticationException("Invalid or expired token"));

        // When
        ResponseEntity<MessageResponse<User>> response = authController.getCurrentUser("Bearer invalid-token");

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Invalid or expired token", response.getBody().message());
        verify(authService).getUserFromToken("Bearer invalid-token");
    }

    @Test
    void shouldReturnInternalServerErrorWhenGetCurrentUserThrowsUnexpectedException() {
        // Given
        when(authService.getUserFromToken(anyString()))
                .thenThrow(new RuntimeException("Database connection error"));

        // When
        ResponseEntity<MessageResponse<User>> response = authController.getCurrentUser("Bearer test-token");

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Failed to retrieve user information", response.getBody().message());
        verify(authService).getUserFromToken("Bearer test-token");
    }

    @Test
    void shouldLogoutSuccessfully() {
        // Given
        doNothing().when(authService).logout(anyString());

        // When
        ResponseEntity<MessageResponse<Void>> response = authController.logout("Bearer test-jwt-token");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Logout successful", response.getBody().message());
        verify(authService).logout("Bearer test-jwt-token");
    }

    @Test
    void shouldHandleLogoutFailure() {
        // Given
        doThrow(new RuntimeException("Logout failed")).when(authService).logout(anyString());

        // When
        ResponseEntity<MessageResponse<Void>> response = authController.logout("Bearer test-jwt-token");

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Logout failed", response.getBody().message());
        verify(authService).logout("Bearer test-jwt-token");
    }

    @Test
    void shouldHandleNullAuthHeaderForGetCurrentUser() {
        // Given
        when(authService.getUserFromToken(null))
                .thenThrow(new AuthenticationException("Invalid or expired token"));

        // When
        ResponseEntity<MessageResponse<User>> response = authController.getCurrentUser(null);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Invalid or expired token", response.getBody().message());
        verify(authService).getUserFromToken(null);
    }

    @Test
    void shouldHandleNullAuthHeaderForLogout() {
        // Given
        doThrow(new RuntimeException("Invalid token")).when(authService).logout(null);

        // When
        ResponseEntity<MessageResponse<Void>> response = authController.logout(null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("Logout failed", response.getBody().message());
        verify(authService).logout(null);
    }

    @Test
    void shouldVerifyCorrectExceptionTypesAreHandled() {
        // Test that ValidationException is properly handled
        doThrow(new ValidationException("Validation failed"))
                .when(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));

        ResponseEntity<MessageResponse<AuthResponse>> response = authController.register(registerRequest, bindingResult);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().message());
    }

    @Test
    void shouldVerifyAuthenticationExceptionIsHandled() {
        // Test that AuthenticationException is properly handled
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("Authentication failed"));

        ResponseEntity<MessageResponse<AuthResponse>> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Authentication failed", response.getBody().message());
    }

    @Test
    void shouldVerifyIllegalArgumentExceptionIsHandled() {
        // Test that IllegalArgumentException is properly handled
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid argument"));

        doNothing().when(authValidator).validateRegistrationRequest(any(RegisterRequest.class), any(BindingResult.class));

        ResponseEntity<MessageResponse<AuthResponse>> response = authController.register(registerRequest, bindingResult);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Invalid argument", response.getBody().message());
    }
}
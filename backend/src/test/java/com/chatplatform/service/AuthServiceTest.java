package com.chatplatform.service;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserService userService;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        
        loginRequest = new LoginRequest("test@example.com", "password123");
        
        registerRequest = new RegisterRequest("newuser", "new@example.com", "password123", "New User");
    }
    
    @Test
    void shouldLoginSuccessfully() {
        // Given
        when(userService.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(jwtService.generateToken(testUser)).thenReturn("test-jwt-token");
        
        // When
        AuthResponse response = authService.login(loginRequest);
        
        // Then
        assertNotNull(response);
        assertEquals("test-jwt-token", response.token());
        assertEquals(testUser.getUsername(), response.user().username());
        assertEquals(testUser.getEmail(), response.user().email());
        assertTrue(response.isAuthenticated());
        
        verify(userService).updateUserOnlineStatus(testUser.getId(), true);
        verify(userService).findByEmail(loginRequest.email());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(testUser);
    }
    
    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userService.findByEmail(loginRequest.email())).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(userService).findByEmail(loginRequest.email());
        verifyNoInteractions(authenticationManager, jwtService);
    }
    
    @Test
    void shouldRegisterSuccessfully() {
        // Given
        when(userService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("test-jwt-token");
        
        // When
        AuthResponse response = authService.register(registerRequest);
        
        // Then
        assertNotNull(response);
        assertEquals("test-jwt-token", response.token());
        assertEquals(testUser.getUsername(), response.user().username());
        assertEquals(testUser.getEmail(), response.user().email());
        assertTrue(response.isAuthenticated());
        
        verify(userService).createUser(
                registerRequest.username(),
                registerRequest.email(),
                registerRequest.password(),
                registerRequest.displayName()
        );
        verify(userService).updateUserOnlineStatus(testUser.getId(), true);
        verify(jwtService).generateToken(testUser);
    }
    
    @Test
    void shouldThrowExceptionWhenRegistrationFails() {
        // Given
        when(userService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username already exists"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });
        
        assertEquals("Username already exists", exception.getMessage());
        verify(userService).createUser(anyString(), anyString(), anyString(), anyString());
        verifyNoInteractions(jwtService);
    }
    
    @Test
    void shouldGetUserFromValidToken() {
        // Given
        String token = "Bearer valid-jwt-token";
        when(jwtService.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn(testUser.getUsername());
        when(userService.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        
        // When
        User result = authService.getUserFromToken(token);
        
        // Then
        assertEquals(testUser, result);
        verify(jwtService).validateToken("valid-jwt-token");
        verify(jwtService).extractUsername("valid-jwt-token");
        verify(userService).findByUsername(testUser.getUsername());
    }
    
    @Test
    void shouldThrowExceptionForInvalidToken() {
        // Given
        String token = "Bearer invalid-jwt-token";
        when(jwtService.validateToken("invalid-jwt-token")).thenReturn(false);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.getUserFromToken(token);
        });
        
        assertEquals("Invalid or expired token", exception.getMessage());
        verify(jwtService).validateToken("invalid-jwt-token");
        verifyNoMoreInteractions(jwtService, userService);
    }
    
    @Test
    void shouldThrowExceptionForEmptyToken() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.getUserFromToken("");
        });
        
        assertEquals("Invalid or expired token", exception.getMessage());
        verifyNoInteractions(jwtService, userService);
    }
    
    @Test
    void shouldLogoutSuccessfully() {
        // Given
        String token = "Bearer valid-jwt-token";
        when(jwtService.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn(testUser.getUsername());
        when(userService.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        
        // When
        assertDoesNotThrow(() -> authService.logout(token));
        
        // Then
        verify(userService).updateUserOnlineStatus(testUser.getId(), false);
    }
    
    @Test
    void shouldHandleLogoutWithInvalidToken() {
        // Given
        String token = "Bearer invalid-jwt-token";
        
        // When & Then
        assertDoesNotThrow(() -> authService.logout(token));
        
        // Logout should not throw exception even with invalid token
        // The safeGetUserFromToken method handles this gracefully
    }
    
    @Test
    void shouldValidateLoginRequest() {
        // Given
        LoginRequest validRequest = new LoginRequest("test@example.com", "password123");
        LoginRequest invalidRequest = new LoginRequest("", "");
        
        // When & Then
        assertTrue(validRequest.isValid());
        assertFalse(invalidRequest.isValid());
    }
    
    @Test
    void shouldValidateRegisterRequest() {
        // Given
        RegisterRequest validRequest = new RegisterRequest("user", "test@example.com", "password123", "Test User");
        RegisterRequest invalidRequest = new RegisterRequest("", "", "", "");
        
        // When & Then
        assertTrue(validRequest.isValid());
        assertFalse(invalidRequest.isValid());
    }
    
    @Test
    void shouldCreateMaskedRegisterRequest() {
        // Given
        RegisterRequest request = new RegisterRequest("user", "test@example.com", "password123", "Test User");
        
        // When
        RegisterRequest masked = request.withMaskedPassword();
        
        // Then
        assertEquals("user", masked.username());
        assertEquals("test@example.com", masked.email());
        assertEquals("****", masked.password());
        assertEquals("Test User", masked.displayName());
    }
}
package com.chatplatform.service.interfaces;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;

/**
 * Service interface for authentication operations.
 * Defines the contract for user authentication, registration, and token management.
 */
public interface AuthService {
    
    /**
     * Authenticate user with email and password
     * 
     * @param loginRequest containing email and password
     * @return AuthResponse with user details and JWT token
     * @throws com.chatplatform.exception.AuthenticationException if credentials are invalid
     */
    AuthResponse login(LoginRequest loginRequest);
    
    /**
     * Register a new user account
     * 
     * @param registerRequest containing user registration details
     * @return AuthResponse with user details and JWT token
     * @throws com.chatplatform.exception.ValidationException if registration data is invalid
     */
    AuthResponse register(RegisterRequest registerRequest);
    
    /**
     * Extract user information from JWT token
     * 
     * @param token JWT token
     * @return User object if token is valid
     * @throws com.chatplatform.exception.AuthenticationException if token is invalid
     */
    User getUserFromToken(String token);
    
    /**
     * Validate JWT token
     * 
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    boolean isTokenValid(String token);
    
    /**
     * Invalidate user session (logout)
     * 
     * @param token JWT token to invalidate
     */
    void logout(String token);
    
    /**
     * Refresh JWT token
     * 
     * @param token current JWT token
     * @return new JWT token
     * @throws com.chatplatform.exception.AuthenticationException if token cannot be refreshed
     */
    String refreshToken(String token);
}
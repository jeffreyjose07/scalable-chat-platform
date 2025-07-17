package com.chatplatform.service;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
import com.chatplatform.exception.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Authentication service using Java 17 functional programming patterns
 * Provides secure user authentication with proper error handling
 */
@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
    public AuthService(UserService userService, 
                      JwtService jwtService,
                      AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }
    
    /**
     * Authenticate user with functional approach
     */
    public AuthResponse login(LoginRequest loginRequest) {
        logger.info("🔐 Attempting login for email: {}", loginRequest.email());
        
        return findUserByEmail(loginRequest.email())
                .map(user -> authenticateUser(user, loginRequest.password()))
                .map(this::generateAuthResponse)
                .map(this::setUserOnline)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
    }
    
    /**
     * Register new user with functional approach
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        logger.info("📝 Attempting registration for email: {}", registerRequest.email());
        
        try {
            User user = createUser(registerRequest).orElseThrow(() -> 
                new AuthenticationException("Registration failed"));
            
            AuthResponse authResponse = generateAuthResponse(user);
            return setUserOnline(authResponse);
        } catch (IllegalArgumentException e) {
            // Wrap in AuthenticationException for consistent exception handling
            logger.warn("Registration failed: {}", e.getMessage());
            throw new AuthenticationException(e.getMessage());
        }
    }
    
    /**
     * Extract user from token with functional validation
     */
    public User getUserFromToken(String token) {
        return Optional.ofNullable(token)
                .filter(t -> !t.isEmpty())
                .map(this::removeBearerPrefix)
                .filter(jwtService::validateToken)
                .map(jwtService::extractUsername)
                .flatMap(userService::findByUsername)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired token"));
    }
    
    /**
     * Logout user with graceful error handling
     */
    public void logout(String token) {
        Optional.ofNullable(token)
                .map(this::safeGetUserFromToken)
                .ifPresentOrElse(
                    user -> {
                        userService.updateUserOnlineStatus(user.getId(), false);
                        logger.info("👋 Logout successful for user: {}", user.getUsername());
                    },
                    () -> logger.warn("⚠️ Logout failed: Invalid token")
                );
    }
    
    /**
     * Send password reset email (placeholder implementation)
     */
    public void sendPasswordResetEmail(String email) {
        // For now, just log the reset request
        // In production, you would:
        // 1. Generate a secure reset token
        // 2. Store it in the database with expiration
        // 3. Send email with reset link
        logger.info("🔄 Password reset requested for email: {}", email);
        
        // Simulate password reset token generation
        findUserByEmail(email).ifPresent(user -> {
            String resetToken = jwtService.generateToken(user);
            logger.info("📧 Password reset token generated for user: {} (token: {}...)", user.getUsername(), resetToken.substring(0, 10));
            // In production: store token and send email with reset identifier
        });
    }
    
    /**
     * Reset password using token (placeholder implementation)
     */
    public void resetPassword(String token, String newPassword) {
        // For now, simple validation
        // In production, you would:
        // 1. Validate the reset token
        // 2. Check token expiration
        // 3. Update user password
        logger.info("🔄 Password reset attempt with token: {}...", token.substring(0, 10));
        
        if (token == null || token.length() < 10) {
            throw new AuthenticationException("Invalid reset token");
        }
        
        // Simulate password reset
        logger.info("✅ Password reset successful (simulated)");
    }
    
    /**
     * Async login for better performance
     */
    public CompletableFuture<AuthResponse> loginAsync(LoginRequest loginRequest) {
        return CompletableFuture.supplyAsync(() -> login(loginRequest));
    }
    
    /**
     * Async registration for better performance
     */
    public CompletableFuture<AuthResponse> registerAsync(RegisterRequest registerRequest) {
        return CompletableFuture.supplyAsync(() -> register(registerRequest));
    }
    
    // Private helper methods using functional patterns
    
    /**
     * Find user by email with error handling
     */
    private Optional<User> findUserByEmail(String email) {
        return userService.findByEmail(email);
    }
    
    /**
     * Authenticate user credentials
     */
    private User authenticateUser(User user, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), password)
        );
        
        if (authentication.isAuthenticated()) {
            logger.info("✅ Authentication successful for user: {}", user.getUsername());
            return user;
        } else {
            throw new AuthenticationException("Authentication failed");
        }
    }
    
    /**
     * Generate authentication response using Builder pattern
     */
    private AuthResponse generateAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        logger.info("🎫 JWT token generated for user: {}", user.getUsername());
        
        return AuthResponse.builder()
            .token(token)
            .user(user)
            .successful()
            .build();
    }
    
    /**
     * Set user online status
     */
    private AuthResponse setUserOnline(AuthResponse authResponse) {
        if (authResponse.isAuthenticated()) {
            userService.updateUserOnlineStatus(authResponse.user().id(), true);
            logger.info("🟢 User set to online: {}", authResponse.user().username());
        }
        return authResponse;
    }
    
    /**
     * Create new user with validation
     */
    private Optional<User> createUser(RegisterRequest registerRequest) {
        User user = userService.createUser(
                registerRequest.username(),
                registerRequest.email(),
                registerRequest.password(),
                registerRequest.displayName()
        );
        logger.info("👤 User created successfully: {}", user.getUsername());
        return Optional.of(user);
    }
    
    /**
     * Remove Bearer prefix from token
     */
    private String removeBearerPrefix(String token) {
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }
    
    /**
     * Safe user extraction from token (no exceptions thrown)
     */
    private User safeGetUserFromToken(String token) {
        try {
            return getUserFromToken(token);
        } catch (Exception e) {
            logger.warn("⚠️ Failed to extract user from token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Functional validation chain builder
     */
    private <T> Function<T, T> validate(java.util.function.Predicate<T> predicate, Supplier<AuthenticationException> exceptionSupplier) {
        return input -> {
            if (predicate.test(input)) {
                return input;
            } else {
                throw exceptionSupplier.get();
            }
        };
    }
}
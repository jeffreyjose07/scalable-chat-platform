package com.chatplatform.service;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
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
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
    }
    
    /**
     * Register new user with functional approach
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        logger.info("📝 Attempting registration for email: {}", registerRequest.email());
        
        try {
            User user = createUser(registerRequest).orElseThrow(() -> 
                new RuntimeException("Registration failed"));
            
            AuthResponse authResponse = generateAuthResponse(user);
            return setUserOnline(authResponse);
        } catch (IllegalArgumentException e) {
            // Re-throw with original message for proper error handling
            throw new RuntimeException(e.getMessage(), e);
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
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));
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
            throw new RuntimeException("Authentication failed");
        }
    }
    
    /**
     * Generate authentication response
     */
    private AuthResponse generateAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        logger.info("🎫 JWT token generated for user: {}", user.getUsername());
        return AuthResponse.success(token, user);
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
    private <T> Function<T, T> validate(java.util.function.Predicate<T> predicate, Supplier<RuntimeException> exceptionSupplier) {
        return input -> {
            if (predicate.test(input)) {
                return input;
            } else {
                throw exceptionSupplier.get();
            }
        };
    }
}
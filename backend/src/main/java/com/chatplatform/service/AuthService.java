package com.chatplatform.service;

import com.chatplatform.dto.AuthResponse;
import com.chatplatform.dto.LoginRequest;
import com.chatplatform.dto.RegisterRequest;
import com.chatplatform.model.User;
import com.chatplatform.exception.AuthenticationException;
import com.chatplatform.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetTokenService passwordResetTokenService;
    
    public AuthService(UserService userService, 
                      JwtService jwtService,
                      AuthenticationManager authenticationManager,
                      TokenBlacklistService tokenBlacklistService,
                      PasswordEncoder passwordEncoder,
                      EmailService emailService,
                      PasswordResetTokenService passwordResetTokenService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.passwordResetTokenService = passwordResetTokenService;
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
     * Logout user with token blacklisting and graceful error handling
     */
    public void logout(String token) {
        Optional.ofNullable(token)
                .map(this::removeBearerPrefix)
                .ifPresentOrElse(
                    cleanToken -> {
                        // Blacklist the token first
                        tokenBlacklistService.blacklistToken(cleanToken);
                        
                        // Then update user status
                        Optional.ofNullable(safeGetUserFromToken("Bearer " + cleanToken))
                                .ifPresentOrElse(
                                    user -> {
                                        userService.updateUserOnlineStatus(user.getId(), false);
                                        logger.info("👋 Logout successful for user: {} (token blacklisted)", user.getUsername());
                                    },
                                    () -> logger.info("🚫 Token blacklisted successfully (user not found)")
                                );
                    },
                    () -> logger.warn("⚠️ Logout failed: Invalid token")
                );
    }
    
    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String email) {
        logger.info("🔄 Password reset requested for email: {}", email);
        
        try {
            // Check rate limiting
            if (passwordResetTokenService.isRateLimitExceeded(email)) {
                logger.warn("Rate limit exceeded for password reset requests: {}", email);
                // Still return success for security (don't reveal rate limit status)
                return;
            }
            
            // Find user by email
            Optional<User> userOptional = findUserByEmail(email);
            
            if (userOptional.isEmpty()) {
                logger.info("Password reset requested for non-existent email (security: not revealing)");
                // Return success to prevent email enumeration
                return;
            }
            
            User user = userOptional.get();
            
            // Generate reset token
            String resetToken = passwordResetTokenService.generateToken(user.getId());
            
            // Send email
            emailService.sendPasswordResetEmail(email, resetToken, user.getDisplayName());
            
            logger.info("📧 Password reset email sent successfully to: {}", email);
            
        } catch (Exception e) {
            logger.error("Failed to send password reset email for: {}", email, e);
            // Don't throw exception - return success for security
        }
    }
    
    /**
     * Reset password using token
     */
    public void resetPassword(String token, String newPassword) {
        logger.info("🔄 Password reset attempt");
        
        // Validate token and get user ID
        String userId = passwordResetTokenService.validateAndConsumeToken(token);
        
        if (userId == null) {
            logger.warn("Invalid or expired password reset token");
            throw new AuthenticationException("Invalid or expired reset token");
        }
        
        // Find user
        Optional<User> userOptional = userService.findById(userId);
        
        if (userOptional.isEmpty()) {
            logger.error("User not found for ID: {}", userId);
            throw new AuthenticationException("User not found");
        }
        
        User user = userOptional.get();
        
        // Validate new password strength (reuse existing validation from changePassword)
        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }
        
        // Update password
        String encodedPassword = passwordEncoder.encode(newPassword);
        userService.updateUserPassword(user.getId(), encodedPassword);
        
        logger.info("✅ Password reset successful for user: {}", user.getUsername());
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
     * Change user password with security validation
     */
    public void changePassword(String userId, String currentPassword, String newPassword) {
        logger.info("Password change requested for user: {}", userId);
        
        try {
            // Find user by username (userId in our case is username)
            Optional<User> userOptional = userService.findByUsername(userId);
            if (userOptional.isEmpty()) {
                throw new AuthenticationException("User not found");
            }
            
            User user = userOptional.get();
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                logger.warn("Password change failed - incorrect current password for user: {}", userId);
                throw new AuthenticationException("Current password is incorrect");
            }
            
            // Ensure new password is different
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new ValidationException("New password must be different from current password");
            }
            
            // Update password
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            userService.updateUserPassword(user.getId(), encodedNewPassword);
            
            logger.info("Password changed successfully for user: {}", userId);
            
        } catch (AuthenticationException | ValidationException e) {
            throw e; // Re-throw expected exceptions
        } catch (Exception e) {
            logger.error("Unexpected error during password change for user: {}", userId, e);
            throw new RuntimeException("Password change failed", e);
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
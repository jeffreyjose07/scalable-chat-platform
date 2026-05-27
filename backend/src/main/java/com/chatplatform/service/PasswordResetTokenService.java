package com.chatplatform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class PasswordResetTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenService.class);
    private static final String TOKEN_PREFIX = "password-reset:";
    private static final Duration TOKEN_EXPIRATION = Duration.ofMinutes(30);
    private static final String RATE_LIMIT_PREFIX = "password-reset-rate:";
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_HOUR = 5;
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public PasswordResetTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Generate a new password reset token for a user
     * 
     * @param userId The user ID
     * @return The generated token
     */
    public String generateToken(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = TOKEN_PREFIX + token;
        
        try {
            redisTemplate.opsForValue().set(key, userId, TOKEN_EXPIRATION);
            logger.info("Generated password reset token for user (expires in {} minutes)", TOKEN_EXPIRATION.toMinutes());
            return token;
        } catch (Exception e) {
            logger.error("Failed to store password reset token in Redis: {}", e.getMessage());
            throw new RuntimeException("Failed to generate password reset token", e);
        }
    }
    
    /**
     * Validate and consume a password reset token
     * 
     * @param token The token to validate
     * @return The user ID if token is valid, null otherwise
     */
    public String validateAndConsumeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Empty or null token provided for validation");
            return null;
        }
        
        String key = TOKEN_PREFIX + token;
        
        try {
            String userId = redisTemplate.opsForValue().get(key);
            
            if (userId != null) {
                // Delete token immediately (single use)
                redisTemplate.delete(key);
                logger.info("Password reset token validated and consumed for user");
                return userId;
            } else {
                logger.warn("Invalid or expired password reset token: {}", token.substring(0, Math.min(8, token.length())) + "...");
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to validate password reset token: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if user has exceeded rate limit for password reset requests
     * 
     * @param email The user's email
     * @return true if rate limit exceeded, false otherwise
     */
    public boolean isRateLimitExceeded(String email) {
        String key = RATE_LIMIT_PREFIX + email;
        
        try {
            String count = redisTemplate.opsForValue().get(key);
            
            if (count == null) {
                redisTemplate.opsForValue().set(key, "1", RATE_LIMIT_WINDOW);
                return false;
            }
            
            int requestCount = Integer.parseInt(count);
            
            if (requestCount >= MAX_REQUESTS_PER_HOUR) {
                logger.warn("Rate limit exceeded for password reset requests: {}", email);
                return true;
            }
            
            redisTemplate.opsForValue().increment(key);
            return false;
            
        } catch (Exception e) {
            logger.error("Failed to check rate limit: {}", e.getMessage());
            // Allow request on error (fail open for better UX)
            return false;
        }
    }
}

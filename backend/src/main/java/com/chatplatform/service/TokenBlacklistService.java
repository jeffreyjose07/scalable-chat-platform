package com.chatplatform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing JWT token blacklisting using Redis
 * Provides secure token revocation capabilities for logout and security incidents
 */
@Service
public class TokenBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtService jwtService;
    
    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }
    
    /**
     * Blacklist a JWT token by its ID (jti claim)
     * Token will be blacklisted until its natural expiration
     */
    public void blacklistToken(String token) {
        try {
            String jwtId = jwtService.extractJwtId(token);
            if (jwtId != null) {
                // Calculate remaining TTL until token expires
                long expirationTime = jwtService.extractExpiration(token).getTime();
                long currentTime = System.currentTimeMillis();
                long ttlSeconds = (expirationTime - currentTime) / 1000;
                
                if (ttlSeconds > 0) {
                    String blacklistKey = BLACKLIST_PREFIX + jwtId;
                    redisTemplate.opsForValue().set(blacklistKey, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
                    logger.info("Token blacklisted successfully: jti={}, ttl={}s", jwtId, ttlSeconds);
                } else {
                    logger.debug("Token already expired, no need to blacklist: jti={}", jwtId);
                }
            } else {
                logger.warn("Cannot blacklist token: missing jti claim");
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist token", e);
        }
    }
    
    /**
     * Check if a JWT token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String jwtId = jwtService.extractJwtId(token);
            if (jwtId != null) {
                String blacklistKey = BLACKLIST_PREFIX + jwtId;
                Boolean exists = redisTemplate.hasKey(blacklistKey);
                boolean isBlacklisted = exists != null && exists;
                
                if (isBlacklisted) {
                    logger.debug("Token is blacklisted: jti={}", jwtId);
                }
                
                return isBlacklisted;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error checking token blacklist status, failing open: {}", e.getMessage());
            // Fail open: if we can't check blacklist (e.g., Redis down), allow the token
            // This prioritizes availability over perfect security
            return false;
        }
    }
    
    /**
     * Blacklist all tokens for a specific user
     * Useful for security incidents or account compromise
     */
    public void blacklistAllUserTokens(String username) {
        // This would require keeping track of all user tokens
        // For now, we'll just log the action - full implementation would need user session tracking
        logger.info("Request to blacklist all tokens for user: {}", username);
        // Note: Full implementation requires user session tracking system
        // Current design focuses on individual token blacklisting for security
    }
    
    /**
     * Get blacklist statistics (for monitoring)
     */
    public long getBlacklistedTokenCount() {
        try {
            return redisTemplate.keys(BLACKLIST_PREFIX + "*").size();
        } catch (Exception e) {
            logger.error("Error getting blacklist count", e);
            return -1;
        }
    }
    
    /**
     * Clear expired entries from blacklist (cleanup job)
     * Redis TTL handles this automatically, but this method is for monitoring
     */
    public void cleanupExpiredBlacklistEntries() {
        // Redis automatically removes expired keys, but we can log cleanup stats
        long count = getBlacklistedTokenCount();
        logger.debug("Current blacklisted token count: {}", count);
    }
}
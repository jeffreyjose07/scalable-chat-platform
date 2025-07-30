package com.chatplatform.service;

import com.chatplatform.security.RateLimitingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for maintaining security-related components
 * Performs cleanup tasks to prevent memory leaks and maintain performance
 */
@Service
public class SecurityMaintenanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityMaintenanceService.class);
    
    @Autowired
    private RateLimitingFilter rateLimitingFilter;
    
    /**
     * Clean up old rate limiting entries every 5 minutes
     * Prevents memory leaks from accumulating IP tracking data
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupRateLimitingEntries() {
        try {
            logger.debug("Starting rate limiting cleanup");
            rateLimitingFilter.cleanupOldEntries();
            logger.debug("Rate limiting cleanup completed");
        } catch (Exception e) {
            logger.error("Error during rate limiting cleanup", e);
        }
    }
    
    /**
     * Log security statistics every hour for monitoring
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logSecurityStatistics() {
        try {
            // This can be extended to log various security metrics
            logger.info("Security maintenance: Periodic security check completed");
        } catch (Exception e) {
            logger.error("Error during security statistics logging", e);
        }
    }
}
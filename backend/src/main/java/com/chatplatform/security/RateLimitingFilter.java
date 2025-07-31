package com.chatplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter to prevent DDoS and brute force attacks
 * Implements sliding window rate limiting per IP address
 */
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    // Rate limits per endpoint type
    private static final int AUTH_REQUESTS_PER_MINUTE = 5; // Login/register attempts
    private static final int API_REQUESTS_PER_MINUTE = 100; // General API requests
    private static final int WEBSOCKET_CONNECTIONS_PER_MINUTE = 10; // WebSocket connections
    
    // Sliding window duration in milliseconds (1 minute)
    private static final long WINDOW_DURATION = 60 * 1000;
    
    // Request counters per IP address
    private final Map<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();
        
        // Determine rate limit based on endpoint
        int rateLimit = getRateLimitForEndpoint(requestPath);
        
        if (rateLimit > 0 && isRateLimitExceeded(clientIp, rateLimit)) {
            logger.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestPath);
            
            // Return 429 Too Many Requests
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }
        
        // Continue with the request
        filterChain.doFilter(request, response);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        // Check for X-Forwarded-For header (common in load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check for X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
    
    private int getRateLimitForEndpoint(String requestPath) {
        if (requestPath.startsWith("/api/auth/")) {
            return AUTH_REQUESTS_PER_MINUTE;
        } else if (requestPath.startsWith("/ws/")) {
            return WEBSOCKET_CONNECTIONS_PER_MINUTE;
        } else if (requestPath.startsWith("/api/")) {
            return API_REQUESTS_PER_MINUTE;
        }
        
        // No rate limiting for static resources
        return 0;
    }
    
    private boolean isRateLimitExceeded(String clientIp, int rateLimit) {
        long currentTime = Instant.now().toEpochMilli();
        
        RequestCounter counter = requestCounters.computeIfAbsent(clientIp, k -> new RequestCounter());
        
        synchronized (counter) {
            // Clean up old entries (sliding window)
            counter.timestamps.entrySet().removeIf(entry -> 
                currentTime - entry.getKey() > WINDOW_DURATION
            );
            
            // Count current requests in the window
            int currentRequests = counter.timestamps.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();
            
            if (currentRequests >= rateLimit) {
                return true;
            }
            
            // Add current request
            long timeSlot = currentTime / 1000; // Group by second
            counter.timestamps.computeIfAbsent(timeSlot, k -> new AtomicInteger(0)).incrementAndGet();
            
            return false;
        }
    }
    
    /**
     * Request counter for tracking requests per IP address
     */
    private static class RequestCounter {
        private final Map<Long, AtomicInteger> timestamps = new ConcurrentHashMap<>();
    }
    
    /**
     * Cleanup old entries periodically to prevent memory leaks
     */
    public void cleanupOldEntries() {
        long currentTime = Instant.now().toEpochMilli();
        
        requestCounters.entrySet().removeIf(entry -> {
            RequestCounter counter = entry.getValue();
            synchronized (counter) {
                counter.timestamps.entrySet().removeIf(timeEntry -> 
                    currentTime - timeEntry.getKey() * 1000 > WINDOW_DURATION * 2
                );
                return counter.timestamps.isEmpty();
            }
        });
        
        logger.debug("Cleaned up old rate limiting entries. Active IPs: {}", requestCounters.size());
    }
}
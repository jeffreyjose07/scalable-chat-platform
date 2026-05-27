package com.chatplatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

/**
 * WebSocket handshake interceptor that dynamically allows local network origins
 */
public class DynamicOriginWebSocketConfigurer implements HandshakeInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(DynamicOriginWebSocketConfigurer.class);

    private static final List<Pattern> ALLOWED_ORIGIN_PATTERNS;

    static {
        List<Pattern> patterns = new java.util.ArrayList<>(Arrays.asList(
            // Private IP ranges for local development
            Pattern.compile("^http://192\\.168\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
            Pattern.compile("^http://10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
            Pattern.compile("^http://172\\.(1[6-9]|2[0-9]|3[01])\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
            // Localhost patterns for local development
            Pattern.compile("^https?://localhost:(3000|3001)$"),
            Pattern.compile("^https?://127\\.0\\.0\\.1:(3000|3001)$"),
            // Production domains
            Pattern.compile("^https://.*\\.onrender\\.com$"),
            Pattern.compile("^https://scalable-chat-platform\\.onrender\\.com$")
        ));
        
        // Add origins from environment variable
        String envAllowedOrigins = System.getenv("WEBSOCKET_ALLOWED_ORIGINS");
        if (envAllowedOrigins != null && !envAllowedOrigins.isEmpty()) {
            String[] origins = envAllowedOrigins.split(",");
            for (String origin : origins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    // Escape special characters for regex and allow http/https
                    String patternStr = "^" + Pattern.quote(trimmed).replace("*", ".*") + "$";
                    patterns.add(Pattern.compile(patternStr));
                    LoggerFactory.getLogger(DynamicOriginWebSocketConfigurer.class)
                        .info("Added allowed WebSocket origin from env: {}", trimmed);
                }
            }
        }
        
        ALLOWED_ORIGIN_PATTERNS = java.util.Collections.unmodifiableList(patterns);
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request, 
        ServerHttpResponse response,
        WebSocketHandler wsHandler, 
        Map<String, Object> attributes) {
        
        String origin = request.getHeaders().getOrigin();
        
        if (origin == null) {
            // Allow requests without origin (e.g., from server-side clients)
            return true;
        }
        
        // Check if origin matches any allowed pattern
        boolean isAllowed = ALLOWED_ORIGIN_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(origin).matches());
            
        if (!isAllowed) {
            // For single-service deployment, allow same-origin requests
            String hostHeader = request.getHeaders().getFirst("Host");
            if (hostHeader != null && origin.equals("https://" + hostHeader)) {
                logger.info("WebSocket connection allowed for same-origin: {}", origin);
                return true;
            }
            
            logger.warn("WebSocket connection rejected for origin: {}", origin);
            return false;
        }
        
        logger.info("WebSocket connection allowed for origin: {}", origin);
        return true;
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request, 
        ServerHttpResponse response,
        WebSocketHandler wsHandler, 
        Exception exception) {
        // No additional logic needed after handshake
    }
}
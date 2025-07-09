package com.chatplatform.config;

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

    private static final List<Pattern> ALLOWED_LOCAL_IP_PATTERNS = Arrays.asList(
        // Private IP ranges  
        Pattern.compile("^http://192\\.168\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
        Pattern.compile("^http://10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
        Pattern.compile("^http://172\\.(1[6-9]|2[0-9]|3[01])\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
        // Localhost patterns
        Pattern.compile("^https?://localhost:(3000|3001)$"),
        Pattern.compile("^https?://127\\.0\\.0\\.1:(3000|3001)$")
    );

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request, 
        ServerHttpResponse response,
        WebSocketHandler wsHandler, 
        Map<String, Object> attributes) throws Exception {
        
        String origin = request.getHeaders().getOrigin();
        
        if (origin == null) {
            // Allow requests without origin (e.g., from server-side clients)
            return true;
        }
        
        // Check if origin matches any allowed pattern
        boolean isAllowed = ALLOWED_LOCAL_IP_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(origin).matches());
            
        if (!isAllowed) {
            System.out.println("WebSocket connection rejected for origin: " + origin);
            return false;
        }
        
        System.out.println("WebSocket connection allowed for origin: " + origin);
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
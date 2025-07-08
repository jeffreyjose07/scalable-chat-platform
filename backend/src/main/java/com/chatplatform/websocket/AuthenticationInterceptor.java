package com.chatplatform.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class AuthenticationInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Extract token from query parameters or headers
        String token = extractTokenFromRequest(request);
        
        if (token != null && validateToken(token)) {
            String userId = extractUserIdFromToken(token);
            attributes.put("userId", userId);
            attributes.put("token", token);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Post-handshake logic if needed
    }
    
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // Try to get token from query parameters
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring("token=".length());
                }
            }
        }
        
        // Try to get token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }
        
        return null;
    }
    
    private boolean validateToken(String token) {
        // For now, just check if token is not empty
        // In a real application, validate JWT token here
        return token != null && !token.trim().isEmpty();
    }
    
    private String extractUserIdFromToken(String token) {
        // For now, just return a mock user ID
        // In a real application, extract user ID from JWT token
        return "user_" + token.hashCode();
    }
}
package com.chatplatform.websocket;

import com.chatplatform.service.JwtService;
import com.chatplatform.service.UserService;
import com.chatplatform.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class AuthenticationInterceptor implements HandshakeInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationInterceptor.class);
    
    private final JwtService jwtService;
    private final UserService userService;
    
    public AuthenticationInterceptor(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            // Extract token from query parameters or headers
            String token = extractTokenFromRequest(request);
            logger.info("WebSocket handshake attempt received");
            logger.info("Token present: {}", token != null);
            
            if (token == null) {
                logger.warn("No token provided in WebSocket handshake");
                return false;
            }
            
            // Validate token
            if (!jwtService.validateToken(token)) {
                logger.warn("Invalid JWT token provided");
                return false;
            }
            
            // Extract username from token
            String username = jwtService.extractUsername(token);
            logger.info("Valid token received for authentication");
            
            // Find user in database
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                logger.warn("User not found in database during WebSocket handshake");
                return false;
            }
            
            User user = userOpt.get();
            attributes.put("userId", user.getId());
            attributes.put("username", user.getUsername());
            attributes.put("token", token);
            
            logger.info("WebSocket handshake successful for authenticated user");
            return true;
            
        } catch (Exception e) {
            logger.error("Error during WebSocket handshake", e);
            return false;
        }
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Post-handshake logic if needed
    }
    
    private String extractTokenFromRequest(ServerHttpRequest request) {
        // Try to get token from query parameters
        String query = request.getURI().getQuery();
        if (query != null) {
            return Pattern.compile("token=([^&]*)")
                    .matcher(query)
                    .results()
                    .map(m -> m.group(1))
                    .findFirst()
                    .orElse(null);
        }
        
        // Try to get token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }
        
        return null;
    }
    
}
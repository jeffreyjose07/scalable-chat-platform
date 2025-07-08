package com.chatplatform.config;

import com.chatplatform.websocket.ChatWebSocketHandler;
import com.chatplatform.websocket.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final AuthenticationInterceptor authenticationInterceptor;
    
    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, 
                          AuthenticationInterceptor authenticationInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.authenticationInterceptor = authenticationInterceptor;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:3001", 
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:3001",
                    "https://localhost:3000",
                    "https://localhost:3001"
                )
                .addInterceptors(authenticationInterceptor);
                // Removed SockJS to match frontend plain WebSocket usage
    }
}
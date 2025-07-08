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
                .setAllowedOrigins("*")
                .addInterceptors(authenticationInterceptor);
    }
}
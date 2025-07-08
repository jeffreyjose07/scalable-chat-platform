package com.chatplatform.service;

import com.chatplatform.dto.MessageDistributionEvent;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MessageDistributionService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageDistributionService.class);
    
    private final ConnectionManager connectionManager;
    private final ChatWebSocketHandler webSocketHandler;
    
    public MessageDistributionService(ConnectionManager connectionManager,
                                    ChatWebSocketHandler webSocketHandler) {
        this.connectionManager = connectionManager;
        this.webSocketHandler = webSocketHandler;
    }
    
    @EventListener
    @Async
    public void handleMessageDistribution(MessageDistributionEvent event) {
        try {
            distributeMessage(event.getMessage());
        } catch (Exception e) {
            logger.error("Error distributing message", e);
        }
    }
    
    private void distributeMessage(ChatMessage message) {
        String conversationId = message.getConversationId();
        
        // For now, we'll broadcast to all connected users
        // In a real application, you'd get conversation participants
        Set<String> sessions = connectionManager.getSessionsForServer(getServerId());
        
        sessions.forEach(sessionId -> {
            try {
                webSocketHandler.sendMessageToSession(sessionId, message);
            } catch (Exception e) {
                logger.error("Error sending message to session: {}", sessionId, e);
            }
        });
    }
    
    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
}
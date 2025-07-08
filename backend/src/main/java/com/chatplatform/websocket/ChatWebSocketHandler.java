package com.chatplatform.websocket;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.service.ConnectionManager;
import com.chatplatform.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ConnectionManager connectionManager;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    public ChatWebSocketHandler(ConnectionManager connectionManager, 
                              MessageService messageService,
                              ObjectMapper objectMapper) {
        this.connectionManager = connectionManager;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        String serverId = getServerId();
        
        sessions.put(session.getId(), session);
        connectionManager.registerConnection(userId, serverId, session.getId());
        
        logger.info("WebSocket connection established for user: {}", userId);
        
        // Send pending messages
        List<ChatMessage> pendingMessages = messageService.getPendingMessages(userId);
        if (pendingMessages != null) {
            pendingMessages.forEach(msg -> sendMessage(session, msg));
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatMessage.setSenderId(getUserId(session));
            
            sendAcknowledgment(session, chatMessage.getId());
            
            messageService.processMessage(chatMessage);
            
        } catch (Exception e) {
            logger.error("Error handling message", e);
            sendError(session, "Failed to process message");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        sessions.remove(session.getId());
        connectionManager.unregisterConnection(userId, session.getId());
        
        logger.info("WebSocket connection closed for user: {}", userId);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session: {}", session.getId(), exception);
        super.handleTransportError(session, exception);
    }
    
    public void sendMessageToSession(String sessionId, ChatMessage message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            sendMessage(session, message);
        }
    }
    
    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
    
    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
    
    private void sendMessage(WebSocketSession session, ChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            logger.error("Error sending message", e);
        }
    }
    
    private void sendAcknowledgment(WebSocketSession session, String messageId) {
        try {
            String ackJson = "{\"type\":\"ack\",\"messageId\":\"" + messageId + "\"}";
            session.sendMessage(new TextMessage(ackJson));
        } catch (Exception e) {
            logger.error("Error sending acknowledgment", e);
        }
    }
    
    private void sendError(WebSocketSession session, String error) {
        try {
            String errorJson = "{\"type\":\"error\",\"message\":\"" + error + "\"}";
            session.sendMessage(new TextMessage(errorJson));
        } catch (Exception e) {
            logger.error("Error sending error message", e);
        }
    }
}
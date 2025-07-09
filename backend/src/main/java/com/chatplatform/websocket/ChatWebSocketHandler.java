package com.chatplatform.websocket;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.User;
import com.chatplatform.service.ConnectionManager;
import com.chatplatform.service.MessageService;
import com.chatplatform.service.UserService;
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
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    public ChatWebSocketHandler(ConnectionManager connectionManager, 
                              MessageService messageService,
                              UserService userService,
                              ObjectMapper objectMapper) {
        this.connectionManager = connectionManager;
        this.messageService = messageService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        String username = getUserName(session);
        String serverId = getServerId();
        logger.info("[WS-SESSION] Connection established - Session ID: {}, User ID: {}, Username: {}, Server ID: {}", session.getId(), userId, username, serverId);
        
        logger.info("WebSocket connection established - Session ID: {}, User ID: {}, Username: {}", 
            session.getId(), userId, username);
        
        if (userId == null) {
            logger.error("No userId found in session attributes - closing connection");
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Error closing session", e);
            }
            return;
        }
        
        sessions.put(session.getId(), session);
        connectionManager.registerConnection(userId, serverId, session.getId());
        
        logger.info("WebSocket connection registered for user: {} ({})", username, userId);
        
        // Send pending messages with a small delay to ensure connection is ready
        List<ChatMessage> pendingMessages = messageService.getPendingMessages(userId);
        if (pendingMessages != null && !pendingMessages.isEmpty()) {
            logger.info("Sending {} pending messages to user: {}", pendingMessages.size(), username);
            // Add a small delay to ensure connection is fully established
            new Thread(() -> {
                try {
                    Thread.sleep(100); // Small delay
                    if (session.isOpen()) {
                        pendingMessages.forEach(msg -> {
                            try {
                                sendMessage(session, msg);
                            } catch (Exception e) {
                                logger.warn("Failed to send pending message to user {}: {}", username, e.getMessage());
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            String userId = getUserId(session);
            String username = getUserName(session);
            
            logger.info("Processing message from user: {} ({})", username, userId);
            
            if (userId == null) {
                logger.error("No userId in session - rejecting message");
                sendError(session, "Authentication error");
                return;
            }
            
            // Always set senderId to authenticated user (security measure)
            chatMessage.setSenderId(userId);
            
            // Backend is source of truth for username - always fetch from database
            User sender = userService.findById(userId).orElse(null);
            if (sender != null) {
                chatMessage.setSenderUsername(sender.getUsername());
                logger.debug("Set senderUsername to: {} for user ID: {}", sender.getUsername(), userId);
            } else {
                logger.warn("Could not find user with ID: {}, setting fallback username", userId);
                chatMessage.setSenderUsername("Unknown User");
            }
            
            // Validate required fields
            if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                sendError(session, "Message content cannot be empty");
                return;
            }
            
            if (chatMessage.getConversationId() == null || chatMessage.getConversationId().trim().isEmpty()) {
                sendError(session, "Conversation ID is required");
                return;
            }
            
            logger.info("Processing message from user: {} ({}), content: '{}', conversation: {}", 
                chatMessage.getSenderUsername(), userId, 
                chatMessage.getContent().substring(0, Math.min(50, chatMessage.getContent().length())),
                chatMessage.getConversationId());
            
            sendAcknowledgment(session, chatMessage.getId());
            
            messageService.processMessage(chatMessage);
            
        } catch (Exception e) {
            logger.error("Error handling message from session: {}", session.getId(), e);
            sendError(session, "Failed to process message: " + e.getMessage());
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
            logger.info("[WS-SESSION] Sending message {} to session {} (userId: {})", message.getId(), sessionId, getUserId(session));
            sendMessage(session, message);
        }
    }
    
    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
    
    private String getUserName(WebSocketSession session) {
        return (String) session.getAttributes().get("username");
    }
    
    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
    
    private void sendMessage(WebSocketSession session, ChatMessage message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } else {
                logger.warn("Cannot send message - session is closed: {}", session.getId());
            }
        } catch (Exception e) {
            logger.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
            // Remove session from active sessions if it's broken
            sessions.remove(session.getId());
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
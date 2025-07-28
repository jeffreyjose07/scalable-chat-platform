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
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ConnectionManager connectionManager;
    private final MessageService messageService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConnectionInfo> connectionInfo = new ConcurrentHashMap<>();
    private final ScheduledExecutorService connectionMonitor = Executors.newScheduledThreadPool(2);
    
    // Connection timeout configuration
    private static final long CONNECTION_TIMEOUT_MINUTES = 30;
    private static final long HEARTBEAT_INTERVAL_MINUTES = 5;
    private static final long PING_INTERVAL_MINUTES = 2;
    
    // Connection state tracking
    private static class ConnectionInfo {
        final String userId;
        final String username;
        final Instant connectedAt;
        volatile Instant lastActivity;
        volatile boolean isActive;
        
        ConnectionInfo(String userId, String username) {
            this.userId = userId;
            this.username = username;
            this.connectedAt = Instant.now();
            this.lastActivity = Instant.now();
            this.isActive = true;
        }
        
        void updateActivity() {
            this.lastActivity = Instant.now();
        }
        
        boolean isTimedOut() {
            return Instant.now().isAfter(lastActivity.plusSeconds(CONNECTION_TIMEOUT_MINUTES * 60));
        }
    }
    
    public ChatWebSocketHandler(ConnectionManager connectionManager, 
                              MessageService messageService,
                              UserService userService,
                              ObjectMapper objectMapper) {
        this.connectionManager = connectionManager;
        this.messageService = messageService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        
        // Start connection monitoring
        initializeConnectionMonitor();
    }
    
    private void initializeConnectionMonitor() {
        // Monitor for timed out connections
        connectionMonitor.scheduleAtFixedRate(() -> {
            try {
                cleanupTimedOutConnections();
            } catch (Exception e) {
                logger.warn("Error during connection cleanup: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL_MINUTES, HEARTBEAT_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        // Send periodic ping messages to keep connections alive
        connectionMonitor.scheduleAtFixedRate(() -> {
            try {
                sendHeartbeatPings();
            } catch (Exception e) {
                logger.warn("Error during heartbeat ping: {}", e.getMessage());
            }
        }, PING_INTERVAL_MINUTES, PING_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        logger.info("WebSocket connection monitor started (timeout: {} min, cleanup interval: {} min, ping interval: {} min)", 
            CONNECTION_TIMEOUT_MINUTES, HEARTBEAT_INTERVAL_MINUTES, PING_INTERVAL_MINUTES);
    }
    
    private void cleanupTimedOutConnections() {
        int cleanedUp = 0;
        for (var entry : connectionInfo.entrySet()) {
            String sessionId = entry.getKey();
            ConnectionInfo info = entry.getValue();
            
            if (info.isTimedOut()) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null) {
                    try {
                        logger.debug("Closing timed out connection for user: {} ({})", info.username, info.userId);
                        session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Connection timeout"));
                        cleanedUp++;
                    } catch (Exception e) {
                        logger.debug("Error closing timed out session {}: {}", sessionId, e.getMessage());
                    }
                }
                // Clean up tracking data
                sessions.remove(sessionId);
                connectionInfo.remove(sessionId);
                connectionManager.unregisterConnection(info.userId, sessionId);
            }
        }
        
        if (cleanedUp > 0) {
            logger.debug("Cleaned up {} timed out WebSocket connections", cleanedUp);
        }
    }
    
    private void sendHeartbeatPings() {
        int pingsSent = 0;
        for (var entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();
            ConnectionInfo info = connectionInfo.get(sessionId);
            
            if (session != null && session.isOpen() && info != null && info.isActive) {
                try {
                    // Send ping message
                    sendPing(session);
                    pingsSent++;
                } catch (Exception e) {
                    logger.debug("Failed to send ping to session {}: {}", sessionId, e.getMessage());
                    // Don't remove session here, let normal error handling take care of it
                }
            }
        }
        
        if (pingsSent > 0) {
            logger.debug("Sent {} heartbeat pings to active connections", pingsSent);
        }
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
            logger.error("Session attributes: {}", session.getAttributes());
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("No user ID found"));
            } catch (Exception e) {
                logger.error("Error closing session", e);
            }
            return;
        }
        
        sessions.put(session.getId(), session);
        connectionInfo.put(session.getId(), new ConnectionInfo(userId, username));
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
        // Update activity tracking
        ConnectionInfo info = connectionInfo.get(session.getId());
        if (info != null) {
            info.updateActivity();
        }
        
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
        ConnectionInfo info = connectionInfo.remove(session.getId());
        sessions.remove(session.getId());
        connectionManager.unregisterConnection(userId, session.getId());
        
        if (info != null) {
            long connectionDuration = java.time.Duration.between(info.connectedAt, Instant.now()).toMinutes();
            logger.info("WebSocket connection closed for user: {} ({}) - Duration: {} minutes, Status: {}", 
                info.username, userId, connectionDuration, status);
        } else {
            logger.info("WebSocket connection closed for user: {} - Status: {}", userId, status);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = getUserId(session);
        String username = getUserName(session);
        
        // Handle expected client disconnections with lower log level
        if (isExpectedDisconnection(exception)) {
            logger.debug("WebSocket client disconnected (session: {}, user: {} - {}): {}", 
                session.getId(), username, userId, exception.getClass().getSimpleName());
        } else {
            // Log unexpected errors at ERROR level
            logger.error("WebSocket transport error for session: {} (user: {} - {})", 
                session.getId(), username, userId, exception);
        }
        
        // Clean up session on any transport error
        ConnectionInfo info = connectionInfo.remove(session.getId());
        sessions.remove(session.getId());
        if (userId != null) {
            connectionManager.unregisterConnection(userId, session.getId());
        }
        
        super.handleTransportError(session, exception);
    }
    
    private boolean isExpectedDisconnection(Throwable exception) {
        if (exception == null) return false;
        
        String exceptionName = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        // Common client disconnection scenarios
        return exceptionName.equals("EOFException") ||
               exceptionName.equals("IOException") ||
               exceptionName.equals("SocketException") ||
               exceptionName.equals("ConnectionResetException") ||
               (message != null && (
                   message.contains("Connection reset") ||
                   message.contains("Broken pipe") ||
                   message.contains("Connection closed") ||
                   message.contains("Unexpected end of stream")
               ));
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
            Map<String, Object> ackMessage = new HashMap<>();
            ackMessage.put("type", "ack");
            ackMessage.put("messageId", messageId);
            String ackJson = objectMapper.writeValueAsString(ackMessage);
            session.sendMessage(new TextMessage(ackJson));
        } catch (Exception e) {
            logger.error("Error sending acknowledgment", e);
        }
    }
    
    private void sendError(WebSocketSession session, String error) {
        try {
            Map<String, Object> errorMessage = new HashMap<>();
            errorMessage.put("type", "error");
            errorMessage.put("message", error);
            String errorJson = objectMapper.writeValueAsString(errorMessage);
            session.sendMessage(new TextMessage(errorJson));
        } catch (Exception e) {
            logger.error("Error sending error message", e);
        }
    }
    
    private void sendPing(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                Map<String, Object> pingMessage = new HashMap<>();
                pingMessage.put("type", "ping");
                pingMessage.put("timestamp", System.currentTimeMillis());
                String pingJson = objectMapper.writeValueAsString(pingMessage);
                session.sendMessage(new TextMessage(pingJson));
                logger.debug("Sent ping to session: {}", session.getId());
            } else {
                logger.debug("Cannot send ping - session is closed: {}", session.getId());
            }
        } catch (Exception e) {
            logger.debug("Error sending ping to session {}: {}", session.getId(), e.getMessage());
            // Remove session from active sessions if it's broken
            sessions.remove(session.getId());
            connectionInfo.remove(session.getId());
        }
    }
    
    // Graceful shutdown method
    public void shutdown() {
        logger.info("Shutting down WebSocket connection monitor");
        connectionMonitor.shutdown();
        try {
            if (!connectionMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Connection monitor did not terminate gracefully, forcing shutdown");
                connectionMonitor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for connection monitor shutdown");
            connectionMonitor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
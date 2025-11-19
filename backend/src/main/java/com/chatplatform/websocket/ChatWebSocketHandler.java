package com.chatplatform.websocket;

import com.chatplatform.dto.MessageStatusUpdate;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.service.ConnectionManager;
import com.chatplatform.service.MessageService;
import com.chatplatform.util.Constants;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ConnectionManager connectionManager;
    private final MessageService messageService;
    private final WebSocketMessageDispatcher messageDispatcher;
    private final ObjectMapper objectMapper;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConnectionInfo> connectionInfo = new ConcurrentHashMap<>();
    private final ScheduledExecutorService connectionMonitor = Executors.newScheduledThreadPool(2);
    
    // Connection timeout configuration
    private static final long CONNECTION_TIMEOUT_MINUTES = 30;
    private static final long HEARTBEAT_INTERVAL_MINUTES = 5;
    private static final long PING_INTERVAL_MINUTES = 2;
    private static final long CONNECTION_SETUP_DELAY_MS = 100;
    
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
                              WebSocketMessageDispatcher messageDispatcher,
                              ObjectMapper objectMapper,
                              ConversationParticipantRepository participantRepository,
                              ChatMessageRepository messageRepository) {
        this.connectionManager = connectionManager;
        this.messageService = messageService;
        this.messageDispatcher = messageDispatcher;
        this.objectMapper = objectMapper;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        
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
                        logger.debug("Closing timed out connection for authenticated user");
                        session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Connection timeout"));
                        cleanedUp++;
                    } catch (Exception e) {
                        logger.debug("Error closing timed out session {}: {}", sessionId, e.getMessage());
                    }
                }
                // Clean up tracking data
                sessions.remove(sessionId);
                connectionInfo.remove(sessionId);
                try {
                    connectionManager.unregisterConnection(info.userId, sessionId);
                } catch (Exception e) {
                    logger.warn("Failed to unregister timed out connection from Redis: {}", e.getMessage());
                }
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
        logger.info("[WS-SESSION] Connection established - Session ID: {}, Server ID: {}", session.getId(), serverId);
        
        logger.info("WebSocket connection established - Session ID: {}", session.getId());
        
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
        
        try {
            connectionManager.registerConnection(userId, serverId, session.getId());
            logger.info("WebSocket connection registered for authenticated user");
        } catch (Exception e) {
            logger.error("Failed to register connection in Redis (continuing with local session only): {}", e.getMessage());
            // Continue without Redis - session is still valid locally
        }
        
        // Send pending messages with a small delay to ensure connection is ready
        List<ChatMessage> pendingMessages = messageService.getPendingMessages(userId);
        if (pendingMessages != null && !pendingMessages.isEmpty()) {
            logger.info("Sending {} pending messages to authenticated user", pendingMessages.size());
            // Add a small delay to ensure connection is fully established
            new Thread(() -> {
                try {
                    Thread.sleep(CONNECTION_SETUP_DELAY_MS); // Small delay
                    if (session.isOpen()) {
                        pendingMessages.forEach(msg -> {
                            try {
                                sendMessage(session, msg);
                            } catch (Exception e) {
                                logger.warn("Failed to send pending message to authenticated user: {}", e.getMessage());
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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        updateConnectionActivity(session);
        messageDispatcher.dispatch(session, message.getPayload(), this);
    }
    
    /**
     * Update connection activity tracking
     */
    private void updateConnectionActivity(WebSocketSession session) {
        ConnectionInfo info = connectionInfo.get(session.getId());
        if (info != null) {
            info.updateActivity();
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        ConnectionInfo info = connectionInfo.remove(session.getId());
        sessions.remove(session.getId());
        try {
            connectionManager.unregisterConnection(userId, session.getId());
        } catch (Exception e) {
            logger.warn("Failed to unregister connection from Redis: {}", e.getMessage());
        }
        
        if (info != null) {
            long connectionDuration = java.time.Duration.between(info.connectedAt, Instant.now()).toMinutes();
            logger.info("WebSocket connection closed for authenticated user - Duration: {} minutes, Status: {}", 
                connectionDuration, status);
        } else {
            logger.info("WebSocket connection closed for authenticated user - Status: {}", status);
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
            try {
                connectionManager.unregisterConnection(userId, session.getId());
            } catch (Exception e) {
                logger.warn("Failed to unregister connection from Redis during error handling: {}", e.getMessage());
            }
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
            logger.info("[WS-SESSION] Sending message {} to session {}", message.getId(), sessionId);
            sendMessage(session, message);
        }
    }
    
    /**
     * Broadcast message status update to all participants in the conversation
     */
    public void broadcastMessageStatusUpdate(MessageStatusUpdate statusUpdate) {
        try {
            // Get the conversation ID from the message
            ChatMessage message = messageRepository.findById(statusUpdate.getMessageId()).orElse(null);
            if (message == null) {
                logger.warn("Cannot broadcast status update - message not found: {}", statusUpdate.getMessageId());
                return;
            }
            
            String conversationId = message.getConversationId();
            
            // Create WebSocket message wrapper
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put(Constants.TYPE, statusUpdate.getStatusType() == MessageStatusUpdate.MessageStatusType.DELIVERED 
                ? "MESSAGE_DELIVERED" : "MESSAGE_READ");
            wsMessage.put(Constants.DATA, statusUpdate);
            
            String json = objectMapper.writeValueAsString(wsMessage);
            
            // Get conversation participants and send only to them
            List<ConversationParticipant> participants = participantRepository.findByIdConversationIdAndIsActiveTrue(conversationId);
            Set<String> participantUserIds = participants.stream()
                .map(ConversationParticipant::getUserId)
                .collect(java.util.stream.Collectors.toSet());
                
            int broadcastCount = 0;
            for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
                WebSocketSession session = entry.getValue();
                String sessionUserId = getUserId(session);
                
                // Only send to conversation participants
                if (session.isOpen() && sessionUserId != null && participantUserIds.contains(sessionUserId)) {
                    try {
                        session.sendMessage(new TextMessage(json));
                        logger.debug("Broadcasted status update to participant {} (session: {})", sessionUserId, session.getId());
                        broadcastCount++;
                    } catch (Exception e) {
                        logger.warn("Failed to send status update to participant {} (session {}): {}", 
                            sessionUserId, session.getId(), e.getMessage());
                    }
                }
            }
            
            logger.debug("Broadcasted message status update to {} conversation participants", broadcastCount);
            
        } catch (Exception e) {
            logger.error("Error broadcasting message status update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send status update to specific user's sessions
     */
    public void sendMessageStatusUpdateToUser(String userId, MessageStatusUpdate statusUpdate) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put(Constants.TYPE, statusUpdate.getStatusType() == MessageStatusUpdate.MessageStatusType.DELIVERED 
                ? "MESSAGE_DELIVERED" : "MESSAGE_READ");
            wsMessage.put(Constants.DATA, statusUpdate);
            
            String json = objectMapper.writeValueAsString(wsMessage);
            
            // Find sessions for the specific user
            for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
                WebSocketSession session = entry.getValue();
                if (session.isOpen() && userId.equals(getUserId(session))) {
                    try {
                        session.sendMessage(new TextMessage(json));
                        logger.debug("Sent status update to session: {}", session.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to send status update to session {}: {}", 
                            session.getId(), e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error sending message status update: {}", e.getMessage(), e);
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
    
    private void sendPing(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                Map<String, Object> pingMessage = new HashMap<>();
                pingMessage.put(Constants.TYPE, Constants.PING);
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
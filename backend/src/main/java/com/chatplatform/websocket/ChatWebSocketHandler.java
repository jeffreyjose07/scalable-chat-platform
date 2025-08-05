package com.chatplatform.websocket;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.User;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.dto.MessageStatusUpdate;
import com.chatplatform.dto.WebSocketMessage;
import com.chatplatform.service.ConnectionManager;
import com.chatplatform.service.MessageService;
import com.chatplatform.service.MessageStatusService;
import com.chatplatform.service.UserService;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final MessageStatusService messageStatusService;
    private final UserService userService;
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
                              MessageStatusService messageStatusService,
                              UserService userService,
                              ObjectMapper objectMapper,
                              ConversationParticipantRepository participantRepository,
                              ChatMessageRepository messageRepository) {
        this.connectionManager = connectionManager;
        this.messageService = messageService;
        this.messageStatusService = messageStatusService;
        this.userService = userService;
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
        connectionManager.registerConnection(userId, serverId, session.getId());
        
        logger.info("WebSocket connection registered for authenticated user");
        
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
        // Update activity tracking
        ConnectionInfo info = connectionInfo.get(session.getId());
        if (info != null) {
            info.updateActivity();
        }
        
        try {
            // First check if this is a ping/pong message
            String payload = message.getPayload();
            if (payload.contains("\"type\":\"pong\"") || payload.contains("\"type\":\"ping\"")) {
                logger.debug("Received ping/pong message, ignoring: {}", payload);
                return;
            }
            
            // Check if this is a message status update
            if (payload.contains("\"type\":\"MESSAGE_DELIVERED\"") || payload.contains("\"type\":\"MESSAGE_READ\"")) {
                handleMessageStatusUpdate(session, payload);
                return;
            }
            
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            String userId = getUserId(session);
            String username = getUserName(session);
            
            logger.info("Processing message from authenticated user");
            
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
                logger.debug("Set senderUsername for authenticated user");
            } else {
                logger.warn("Could not find user, setting fallback username");
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
            
            logger.info("Processing message for conversation: {}", chatMessage.getConversationId());
            
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
            logger.info("[WS-SESSION] Sending message {} to session {}", message.getId(), sessionId);
            sendMessage(session, message);
        }
    }
    
    /**
     * Handle incoming message status updates from clients
     */
    private void handleMessageStatusUpdate(WebSocketSession session, String payload) {
        try {
            String userId = getUserId(session);
            if (userId == null) {
                logger.error("No userId in session for status update");
                return;
            }
            
            // First deserialize the payload into a WebSocketMessage wrapper
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            if (wsMessage == null || wsMessage.getData() == null) {
                logger.error("Invalid WebSocket message structure for status update");
                return;
            }
            
            // Extract the data field and deserialize into MessageStatusUpdate
            MessageStatusUpdate statusUpdate = objectMapper.treeToValue(wsMessage.getData(), MessageStatusUpdate.class);
            if (statusUpdate == null) {
                logger.error("Failed to deserialize MessageStatusUpdate from WebSocket message data");
                return;
            }
            
            // Additional null checks
            if (statusUpdate.getMessageId() == null || statusUpdate.getStatusType() == null) {
                logger.error("Invalid status update - messageId or statusType is null. MessageId: {}, StatusType: {}", 
                    statusUpdate.getMessageId(), statusUpdate.getStatusType());
                return;
            }
            
            logger.debug("Received status update from user {}: {} for message {}", 
                userId, statusUpdate.getStatusType(), statusUpdate.getMessageId());
            
            // Validate that the user ID matches the authenticated user
            statusUpdate.setUserId(userId);
            
            // Process the status update
            boolean success = false;
            switch (statusUpdate.getStatusType()) {
                case DELIVERED:
                    success = messageStatusService.updateMessageDeliveryStatus(
                        statusUpdate.getMessageId(), userId);
                    break;
                case READ:
                    success = messageStatusService.updateMessageReadStatus(
                        statusUpdate.getMessageId(), userId);
                    break;
                default:
                    logger.warn("Unknown status type: {}", statusUpdate.getStatusType());
                    return;
            }
            
            if (success) {
                // Broadcast the status update to all participants in the conversation
                broadcastMessageStatusUpdate(statusUpdate);
            } else {
                logger.warn("Failed to update message status for message {} with status {}", 
                    statusUpdate.getMessageId(), statusUpdate.getStatusType());
            }
            
        } catch (Exception e) {
            logger.error("Error handling message status update: {}", e.getMessage(), e);
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
            wsMessage.put("type", statusUpdate.getStatusType() == MessageStatusUpdate.MessageStatusType.DELIVERED 
                ? "MESSAGE_DELIVERED" : "MESSAGE_READ");
            wsMessage.put("data", statusUpdate);
            
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
            wsMessage.put("type", statusUpdate.getStatusType() == MessageStatusUpdate.MessageStatusType.DELIVERED 
                ? "MESSAGE_DELIVERED" : "MESSAGE_READ");
            wsMessage.put("data", statusUpdate);
            
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
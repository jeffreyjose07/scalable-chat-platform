package com.chatplatform.websocket;

import com.chatplatform.dto.MessageStatusUpdate;
import com.chatplatform.dto.WebSocketMessage;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.User;
import com.chatplatform.service.MessageService;
import com.chatplatform.service.MessageStatusService;
import com.chatplatform.service.UserService;
import com.chatplatform.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketMessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageDispatcher.class);

    private final MessageService messageService;
    private final MessageStatusService messageStatusService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public WebSocketMessageDispatcher(MessageService messageService,
                                      MessageStatusService messageStatusService,
                                      UserService userService,
                                      ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.messageStatusService = messageStatusService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public void dispatch(WebSocketSession session, String payload, ChatWebSocketHandler handler) {
        try {
            if (isControlMessage(payload)) {
                handleControlMessage(session, payload, handler);
                return;
            }

            processChatMessage(session, payload, handler);

        } catch (Exception e) {
            logger.error("Error handling message from session: {}", session.getId(), e);
            sendError(session, "Failed to process message: " + e.getMessage());
        }
    }

    private boolean isControlMessage(String payload) {
        return isPingPongMessage(payload) || isStatusUpdateMessage(payload);
    }

    private boolean isPingPongMessage(String payload) {
        return payload.contains("\"type\":\"pong\"") || payload.contains("\"type\":\"ping\"");
    }

    private boolean isStatusUpdateMessage(String payload) {
        return payload.contains("\"type\":\"MESSAGE_DELIVERED\"") || payload.contains("\"type\":\"MESSAGE_READ\"");
    }

    private void handleControlMessage(WebSocketSession session, String payload, ChatWebSocketHandler handler) {
        if (isPingPongMessage(payload)) {
            logger.debug("Received ping/pong message, ignoring: {}", payload);
            return;
        }

        if (isStatusUpdateMessage(payload)) {
            handleMessageStatusUpdate(session, payload, handler);
        }
    }

    private void processChatMessage(WebSocketSession session, String payload, ChatWebSocketHandler handler) throws Exception {
        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
        String userId = getUserId(session);

        logger.info("Processing message from authenticated user");

        if (!isValidSession(session, userId)) {
            return;
        }

        prepareMessage(chatMessage, userId);

        if (!validateMessage(session, chatMessage)) {
            return;
        }

        logger.info("Processing message for conversation: {}", chatMessage.getConversationId());
        sendAcknowledgment(session, chatMessage.getId());
        messageService.processMessage(chatMessage);
    }

    private void handleMessageStatusUpdate(WebSocketSession session, String payload, ChatWebSocketHandler handler) {
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
                handler.broadcastMessageStatusUpdate(statusUpdate);
            } else {
                logger.warn("Failed to update message status for message {} with status {}",
                        statusUpdate.getMessageId(), statusUpdate.getStatusType());
            }

        } catch (Exception e) {
            logger.error("Error handling message status update: {}", e.getMessage(), e);
        }
    }

    private boolean isValidSession(WebSocketSession session, String userId) {
        if (userId == null) {
            logger.error("No userId in session - rejecting message");
            sendError(session, "Authentication error");
            return false;
        }
        return true;
    }

    private void prepareMessage(ChatMessage chatMessage, String userId) {
        chatMessage.setSenderId(userId);

        User sender = userService.findById(userId).orElse(null);
        if (sender != null) {
            chatMessage.setSenderUsername(sender.getUsername());
            logger.debug("Set senderUsername for authenticated user");
        } else {
            logger.warn("Could not find user, setting fallback username");
            chatMessage.setSenderUsername("Unknown User");
        }
    }

    private boolean validateMessage(WebSocketSession session, ChatMessage chatMessage) {
        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            sendError(session, "Message content cannot be empty");
            return false;
        }

        if (chatMessage.getConversationId() == null || chatMessage.getConversationId().trim().isEmpty()) {
            sendError(session, "Conversation ID is required");
            return false;
        }

        return true;
    }

    private void sendAcknowledgment(WebSocketSession session, String messageId) {
        try {
            Map<String, Object> ackMessage = new HashMap<>();
            ackMessage.put(Constants.TYPE, Constants.ACK);
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
            errorMessage.put(Constants.TYPE, Constants.ERROR);
            errorMessage.put(Constants.MESSAGE, error);
            String errorJson = objectMapper.writeValueAsString(errorMessage);
            session.sendMessage(new TextMessage(errorJson));
        } catch (Exception e) {
            logger.error("Error sending error message", e);
        }
    }

    private String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
}

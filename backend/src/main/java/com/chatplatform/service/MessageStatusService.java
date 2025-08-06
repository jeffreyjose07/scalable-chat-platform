package com.chatplatform.service;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.dto.MessageStatusUpdate;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class MessageStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageStatusService.class);
    
    private final ChatMessageRepository messageRepository;
    private final ConversationService conversationService;
    
    public MessageStatusService(ChatMessageRepository messageRepository, 
                              ConversationService conversationService) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
    }
    
    /**
     * Update message delivery status for a specific user
     */
    public boolean updateMessageDeliveryStatus(String messageId, String userId) {
        logger.debug("Updating delivery status for message {} by user {}", messageId, userId);
        
        Optional<ChatMessage> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            logger.warn("Message not found: {}", messageId);
            return false;
        }
        
        ChatMessage message = messageOpt.get();
        
        // Check if user has access to this conversation
        if (!conversationService.hasUserAccess(userId, message.getConversationId())) {
            logger.warn("User {} does not have access to message {}", userId, messageId);
            return false;
        }
        
        // Don't mark as delivered if it's the sender's own message
        if (message.getSenderId().equals(userId)) {
            return true;
        }
        
        message.markAsDeliveredTo(userId);
        messageRepository.save(message);
        
        logger.debug("Message {} marked as delivered to user {}", messageId, userId);
        return true;
    }
    
    /**
     * Update message read status for a specific user
     */
    public boolean updateMessageReadStatus(String messageId, String userId) {
        logger.debug("Updating read status for message {} by user {}", messageId, userId);
        
        Optional<ChatMessage> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            logger.warn("Message not found: {}", messageId);
            return false;
        }
        
        ChatMessage message = messageOpt.get();
        
        // Check if user has access to this conversation
        if (!conversationService.hasUserAccess(userId, message.getConversationId())) {
            logger.warn("User {} does not have access to message {}", userId, messageId);
            return false;
        }
        
        // Don't mark as read if it's the sender's own message
        if (message.getSenderId().equals(userId)) {
            return true;
        }
        
        // Mark as delivered first if not already
        if (!message.isDeliveredTo(userId)) {
            message.markAsDeliveredTo(userId);
        }
        
        message.markAsReadBy(userId);
        messageRepository.save(message);
        
        logger.debug("Message {} marked as read by user {}", messageId, userId);
        return true;
    }
    
    /**
     * Mark all messages in a conversation as read by a user (for existing messages)
     */
    public void markConversationAsRead(String conversationId, String userId) {
        logger.debug("Marking conversation {} as read by user {}", conversationId, userId);
        
        // Check if user has access to this conversation
        if (!conversationService.hasUserAccess(userId, conversationId)) {
            logger.warn("User {} does not have access to conversation {}", userId, conversationId);
            return;
        }
        
        // Get all messages in the conversation that are not sent by this user
        List<ChatMessage> messages = messageRepository.findByConversationIdAndSenderIdNot(conversationId, userId);
        
        int updatedCount = 0;
        for (ChatMessage message : messages) {
            if (!message.isReadBy(userId)) {
                // Mark as delivered first if not already
                if (!message.isDeliveredTo(userId)) {
                    message.markAsDeliveredTo(userId);
                }
                message.markAsReadBy(userId);
                updatedCount++;
            }
        }
        
        if (updatedCount > 0) {
            messageRepository.saveAll(messages);
            logger.debug("Marked {} messages as read in conversation {} by user {}", updatedCount, conversationId, userId);
        }
    }
    
    /**
     * Get message status information for the current user perspective
     */
    public ChatMessage.MessageStatus getMessageStatus(String messageId, String currentUserId) {
        Optional<ChatMessage> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            return ChatMessage.MessageStatus.SENT; // Default fallback
        }
        
        ChatMessage message = messageOpt.get();
        
        // If it's the user's own message, return the actual status
        if (message.getSenderId().equals(currentUserId)) {
            return message.getStatus();
        }
        
        // For other users' messages, they don't see delivery status
        return ChatMessage.MessageStatus.SENT;
    }
    
    /**
     * Batch update message statuses for performance
     */
    public void batchUpdateMessageStatus(List<MessageStatusUpdate> updates) {
        logger.debug("Batch updating {} message statuses", updates.size());
        
        for (MessageStatusUpdate update : updates) {
            try {
                switch (update.getStatusType()) {
                    case DELIVERED:
                        updateMessageDeliveryStatus(update.getMessageId(), update.getUserId());
                        break;
                    case READ:
                        updateMessageReadStatus(update.getMessageId(), update.getUserId());
                        break;
                }
            } catch (Exception e) {
                logger.error("Failed to update status for message {}: {}", update.getMessageId(), e.getMessage());
            }
        }
    }
}
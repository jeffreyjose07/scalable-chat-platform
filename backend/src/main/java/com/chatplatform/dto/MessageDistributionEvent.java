package com.chatplatform.dto;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.ChatMessage.MessageType;
import java.time.Instant;
import java.util.Objects;

/**
 * Message distribution event record using Java 17 features
 * Immutable event data for Kafka messaging
 */
public record MessageDistributionEvent(
    ChatMessage message,
    Instant timestamp,
    String eventId,
    String source
) {
    
    /**
     * Primary constructor
     */
    public MessageDistributionEvent(ChatMessage message) {
        this(message, Instant.now(), generateEventId(), "chat-platform");
    }
    
    /**
     * Compact constructor with validation
     */
    public MessageDistributionEvent {
        Objects.requireNonNull(message, "Message cannot be null");
        timestamp = Objects.requireNonNullElse(timestamp, Instant.now());
        eventId = Objects.requireNonNullElse(eventId, generateEventId());
        source = Objects.requireNonNullElse(source, "chat-platform");
    }
    
    /**
     * Factory method for creating event from message
     */
    public static MessageDistributionEvent from(ChatMessage message) {
        return new MessageDistributionEvent(message);
    }
    
    /**
     * Factory method for creating event with custom source
     */
    public static MessageDistributionEvent from(ChatMessage message, String source) {
        return new MessageDistributionEvent(message, Instant.now(), generateEventId(), source);
    }
    
    /**
     * Check if event is valid for processing
     */
    public boolean isValid() {
        return message != null && 
               message.getId() != null && 
               message.getContent() != null && 
               !message.getContent().isBlank() &&
               timestamp != null &&
               eventId != null;
    }
    
    /**
     * Check if event is recent (within last 5 minutes)
     */
    public boolean isRecent() {
        return timestamp != null && 
               timestamp.isAfter(Instant.now().minusSeconds(300));
    }
    
    /**
     * Get conversation ID from the message
     */
    public String getConversationId() {
        return message != null ? message.getConversationId() : null;
    }
    
    /**
     * Get sender ID from the message
     */
    public String getSenderId() {
        return message != null ? message.getSenderId() : null;
    }
    
    /**
     * Generate unique event ID
     */
    private static String generateEventId() {
        return "evt_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(Objects.hash(Thread.currentThread().getId(), System.nanoTime()));
    }
    
    /**
     * Create a copy with updated timestamp (for retry scenarios)
     */
    public MessageDistributionEvent withTimestamp(Instant newTimestamp) {
        return new MessageDistributionEvent(message, newTimestamp, eventId, source);
    }
    
    /**
     * Create a sanitized version for logging (without sensitive content)
     */
    public MessageDistributionEvent sanitized() {
        if (message == null) {
            return new MessageDistributionEvent(null, timestamp, eventId, source);
        }
        
        // Create a new message with truncated content for logging
        var sanitizedMessage = new ChatMessage(
            message.getConversationId(),
            message.getSenderId(),
            message.getSenderUsername(),
            message.getContent().length() > 50 ? 
                message.getContent().substring(0, 47) + "..." : 
                message.getContent(),
            message.getType()
        );
        sanitizedMessage.setId(message.getId());
        sanitizedMessage.setTimestamp(message.getTimestamp());
        
        return new MessageDistributionEvent(sanitizedMessage, timestamp, eventId, source);
    }
}
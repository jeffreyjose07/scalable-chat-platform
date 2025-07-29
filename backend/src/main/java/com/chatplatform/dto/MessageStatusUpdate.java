package com.chatplatform.dto;

import java.time.Instant;

public class MessageStatusUpdate {
    private String messageId;
    private String userId;
    private MessageStatusType statusType;
    private Instant timestamp;
    
    public enum MessageStatusType {
        DELIVERED,
        READ
    }
    
    public MessageStatusUpdate() {
        this.timestamp = Instant.now();
    }
    
    public MessageStatusUpdate(String messageId, String userId, MessageStatusType statusType) {
        this.messageId = messageId;
        this.userId = userId;
        this.statusType = statusType;
        this.timestamp = Instant.now();
    }
    
    // Getters and setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public MessageStatusType getStatusType() {
        return statusType;
    }
    
    public void setStatusType(MessageStatusType statusType) {
        this.statusType = statusType;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
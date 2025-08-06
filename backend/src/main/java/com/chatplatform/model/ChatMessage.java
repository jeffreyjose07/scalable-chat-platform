package com.chatplatform.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "messages")
public class ChatMessage {
    @Id
    private String id;
    
    @Indexed
    private String conversationId;
    
    @Indexed
    private String senderId;
    
    private String senderUsername;
    
    private String content;
    private MessageType type;
    private Instant timestamp;
    
    @Indexed
    private MessageStatus status = MessageStatus.SENT; // Default status for new messages
    
    // Track delivery status per user (userId -> deliveredAt timestamp)
    private Map<String, Instant> deliveredTo = new HashMap<>();
    
    // Track read status per user (userId -> readAt timestamp)  
    private Map<String, Instant> readBy = new HashMap<>();
    
    @Indexed(expireAfterSeconds = 31536000) // 1 year
    private Instant expiresAt;
    
    public ChatMessage() {
        this.timestamp = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(31536000);
    }
    
    public ChatMessage(String conversationId, String senderId, String senderUsername, String content, MessageType type) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.type = type;
        this.timestamp = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(31536000);
    }
    
    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
    
    public enum MessageStatus {
        PENDING,    // Message being sent/processing
        SENT,       // Message sent to server successfully
        DELIVERED,  // Message delivered to recipient(s)
        READ        // Message read by recipient(s)
    }
    
    // Getters and setters
    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }
    
    public String getConversationId() { 
        return conversationId; 
    }
    
    public void setConversationId(String conversationId) { 
        this.conversationId = conversationId; 
    }
    
    public String getSenderId() { 
        return senderId; 
    }
    
    public void setSenderId(String senderId) { 
        this.senderId = senderId; 
    }
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public String getContent() { 
        return content; 
    }
    
    public void setContent(String content) { 
        this.content = content; 
    }
    
    public MessageType getType() { 
        return type; 
    }
    
    public void setType(MessageType type) { 
        this.type = type; 
    }
    
    public Instant getTimestamp() { 
        return timestamp; 
    }
    
    public void setTimestamp(Instant timestamp) { 
        this.timestamp = timestamp; 
    }
    
    public Instant getExpiresAt() { 
        return expiresAt; 
    }
    
    public void setExpiresAt(Instant expiresAt) { 
        this.expiresAt = expiresAt; 
    }
    
    public MessageStatus getStatus() {
        return status;
    }
    
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    
    public Map<String, Instant> getDeliveredTo() {
        return deliveredTo != null ? deliveredTo : new HashMap<>();
    }
    
    public void setDeliveredTo(Map<String, Instant> deliveredTo) {
        this.deliveredTo = deliveredTo;
    }
    
    public Map<String, Instant> getReadBy() {
        return readBy != null ? readBy : new HashMap<>();
    }
    
    public void setReadBy(Map<String, Instant> readBy) {
        this.readBy = readBy;
    }
    
    // Helper methods for status management
    public void markAsDeliveredTo(String userId) {
        if (this.deliveredTo == null) {
            this.deliveredTo = new HashMap<>();
        }
        this.deliveredTo.put(userId, Instant.now());
        updateOverallStatus();
    }
    
    public void markAsReadBy(String userId) {
        if (this.readBy == null) {
            this.readBy = new HashMap<>();
        }
        this.readBy.put(userId, Instant.now());
        updateOverallStatus();
    }
    
    public boolean isDeliveredTo(String userId) {
        return deliveredTo != null && deliveredTo.containsKey(userId);
    }
    
    public boolean isReadBy(String userId) {
        return readBy != null && readBy.containsKey(userId);
    }
    
    private void updateOverallStatus() {
        // Update the overall message status based on delivery/read tracking
        if (readBy != null && !readBy.isEmpty()) {
            this.status = MessageStatus.READ;
        } else if (deliveredTo != null && !deliveredTo.isEmpty()) {
            this.status = MessageStatus.DELIVERED;
        } else if (this.status == MessageStatus.PENDING) {
            this.status = MessageStatus.SENT;
        }
    }
}
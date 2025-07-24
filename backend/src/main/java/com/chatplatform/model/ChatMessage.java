package com.chatplatform.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

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
}
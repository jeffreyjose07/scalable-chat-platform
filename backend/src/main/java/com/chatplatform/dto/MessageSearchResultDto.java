package com.chatplatform.dto;

import java.time.Instant;

public class MessageSearchResultDto {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderUsername;
    private String content;
    private String highlightedContent;
    private Instant timestamp;
    private double score; // Search relevance score
    
    // Constructors
    public MessageSearchResultDto() {}
    
    public MessageSearchResultDto(String id, String conversationId, String senderId, 
                                 String senderUsername, String content, Instant timestamp) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = timestamp;
        this.highlightedContent = content; // Default to original content
        this.score = 0.0;
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
    
    public String getHighlightedContent() {
        return highlightedContent;
    }
    
    public void setHighlightedContent(String highlightedContent) {
        this.highlightedContent = highlightedContent;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    // Helper methods
    public String getDisplayUsername() {
        return senderUsername != null && !senderUsername.trim().isEmpty() 
            ? senderUsername 
            : senderId;
    }
    
    public boolean hasHighlighting() {
        return highlightedContent != null && !highlightedContent.equals(content);
    }
}
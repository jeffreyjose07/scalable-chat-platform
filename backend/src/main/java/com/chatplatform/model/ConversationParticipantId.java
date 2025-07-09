package com.chatplatform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ConversationParticipantId implements Serializable {
    @Column(name = "conversation_id")
    private String conversationId;
    
    @Column(name = "user_id")
    private String userId;
    
    public ConversationParticipantId() {}
    
    public ConversationParticipantId(String conversationId, String userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }
    
    // Getters and setters
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationParticipantId that = (ConversationParticipantId) o;
        return Objects.equals(conversationId, that.conversationId) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }
}
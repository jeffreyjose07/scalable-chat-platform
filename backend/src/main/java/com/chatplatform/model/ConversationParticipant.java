package com.chatplatform.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conversation_participants")
public class ConversationParticipant {
    @EmbeddedId
    private ConversationParticipantId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "joined_at")
    private Instant joinedAt;
    
    @Column(name = "last_read_at")
    private Instant lastReadAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private ParticipantRole role = ParticipantRole.MEMBER;
    
    public ConversationParticipant() {
        this.joinedAt = Instant.now();
    }
    
    public ConversationParticipant(Conversation conversation, User user) {
        this.id = new ConversationParticipantId(conversation.getId(), user.getId());
        this.conversation = conversation;
        this.user = user;
        this.joinedAt = Instant.now();
        this.isActive = true;
        this.role = ParticipantRole.MEMBER;
    }
    
    public ConversationParticipant(Conversation conversation, User user, ParticipantRole role) {
        this.id = new ConversationParticipantId(conversation.getId(), user.getId());
        this.conversation = conversation;
        this.user = user;
        this.joinedAt = Instant.now();
        this.isActive = true;
        this.role = role;
    }
    
    public ConversationParticipant(String conversationId, String userId) {
        this.id = new ConversationParticipantId(conversationId, userId);
        this.joinedAt = Instant.now();
        this.isActive = true;
        this.role = ParticipantRole.MEMBER;
    }
    
    public ConversationParticipant(String conversationId, String userId, ParticipantRole role) {
        this.id = new ConversationParticipantId(conversationId, userId);
        this.joinedAt = Instant.now();
        this.isActive = true;
        this.role = role;
    }
    
    // Getters and setters
    public ConversationParticipantId getId() {
        return id;
    }
    
    public void setId(ConversationParticipantId id) {
        this.id = id;
    }
    
    public Conversation getConversation() {
        return conversation;
    }
    
    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Instant getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public Instant getLastReadAt() {
        return lastReadAt;
    }
    
    public void setLastReadAt(Instant lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public ParticipantRole getRole() {
        return role;
    }
    
    public void setRole(ParticipantRole role) {
        this.role = role;
    }
    
    // Helper methods
    public String getConversationId() {
        return id != null ? id.getConversationId() : null;
    }
    
    public String getUserId() {
        return id != null ? id.getUserId() : null;
    }
    
    public boolean isOwner() {
        return role == ParticipantRole.OWNER;
    }
    
    public boolean isAdmin() {
        return role == ParticipantRole.ADMIN;
    }
    
    public boolean isOwnerOrAdmin() {
        return role == ParticipantRole.OWNER || role == ParticipantRole.ADMIN;
    }
    
    public boolean canManageParticipants() {
        return isOwnerOrAdmin();
    }
    
    public boolean canUpdateSettings() {
        return isOwnerOrAdmin();
    }
}
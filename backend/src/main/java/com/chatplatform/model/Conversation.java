package com.chatplatform.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type = ConversationType.GROUP;
    
    private String name;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ConversationParticipant> participants = new HashSet<>();
    
    public Conversation() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public Conversation(String id, ConversationType type, String name, String createdBy) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public ConversationType getType() {
        return type;
    }
    
    public void setType(ConversationType type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Set<ConversationParticipant> getParticipants() {
        return participants;
    }
    
    public void setParticipants(Set<ConversationParticipant> participants) {
        this.participants = participants;
    }
    
    // Helper methods
    public void addParticipant(ConversationParticipant participant) {
        participants.add(participant);
        participant.setConversation(this);
    }
    
    public void removeParticipant(ConversationParticipant participant) {
        participants.remove(participant);
        participant.setConversation(null);
    }
}
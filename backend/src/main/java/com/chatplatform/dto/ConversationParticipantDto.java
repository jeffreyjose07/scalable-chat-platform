package com.chatplatform.dto;

import com.chatplatform.model.ParticipantRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * DTO for conversation participants that includes both user information and role
 */
public class ConversationParticipantDto {
    private UserDto user;
    private ParticipantRole role;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    private Instant joinedAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
    private Instant lastReadAt;

    // Default constructor
    public ConversationParticipantDto() {}

    // Constructor with required fields
    public ConversationParticipantDto(UserDto user, ParticipantRole role) {
        this.user = user;
        this.role = role;
    }

    // Full constructor
    public ConversationParticipantDto(UserDto user, ParticipantRole role, Instant joinedAt, Instant lastReadAt) {
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
        this.lastReadAt = lastReadAt;
    }

    // Getters and setters
    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
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

    @Override
    public String toString() {
        return "ConversationParticipantDto{" +
                "user=" + user +
                ", role=" + role +
                ", joinedAt=" + joinedAt +
                ", lastReadAt=" + lastReadAt +
                '}';
    }
}
package com.chatplatform.dto;

import com.chatplatform.model.ConversationType;
import java.time.Instant;
import java.util.List;

public class ConversationDto {
    private String id;
    private ConversationType type;
    private String name;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ConversationParticipantDto> participants;
    private String lastMessageContent;
    private Instant lastMessageAt;
    private String lastMessageSender;
    private int unreadCount;
    private String description;
    private Boolean isPublic;
    private Integer maxParticipants;
    
    // Constructors
    public ConversationDto() {}
    
    public ConversationDto(String id, ConversationType type, String name, String createdBy, 
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.unreadCount = 0;
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
    
    public List<ConversationParticipantDto> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<ConversationParticipantDto> participants) {
        this.participants = participants;
    }
    
    public String getLastMessageContent() {
        return lastMessageContent;
    }
    
    public void setLastMessageContent(String lastMessageContent) {
        this.lastMessageContent = lastMessageContent;
    }
    
    public Instant getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
    
    public String getLastMessageSender() {
        return lastMessageSender;
    }
    
    public void setLastMessageSender(String lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public Integer getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    // Helper methods
    public boolean isDirectMessage() {
        return ConversationType.DIRECT.equals(type);
    }
    
    public boolean isGroup() {
        return ConversationType.GROUP.equals(type);
    }
    
    // For direct messages, get the other participant
    public UserDto getOtherParticipant(String currentUserId) {
        if (!isDirectMessage() || participants == null || participants.size() != 2) {
            return null;
        }
        
        return participants.stream()
            .map(ConversationParticipantDto::getUser)
            .filter(user -> !user.getId().equals(currentUserId))
            .findFirst()
            .orElse(null);
    }
    
    // Get display name for conversation
    public String getDisplayName(String currentUserId) {
        if (isGroup()) {
            return name;
        }
        
        UserDto otherUser = getOtherParticipant(currentUserId);
        return otherUser != null ? otherUser.getDisplayName() : "Unknown User";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationDto that)) return false;
        
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
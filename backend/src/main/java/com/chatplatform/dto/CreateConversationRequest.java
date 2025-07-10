package com.chatplatform.dto;

import com.chatplatform.model.ConversationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateConversationRequest {
    
    @NotBlank(message = "Conversation name is required")
    @Size(max = 100, message = "Conversation name cannot exceed 100 characters")
    private String name;
    
    @NotNull(message = "Conversation type is required")
    private ConversationType type;
    
    @Size(min = 1, message = "At least one participant is required")
    private List<String> participantIds;
    
    // Constructors
    public CreateConversationRequest() {}
    
    public CreateConversationRequest(String name, ConversationType type, List<String> participantIds) {
        this.name = name;
        this.type = type;
        this.participantIds = participantIds;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ConversationType getType() {
        return type;
    }
    
    public void setType(ConversationType type) {
        this.type = type;
    }
    
    public List<String> getParticipantIds() {
        return participantIds;
    }
    
    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }
}
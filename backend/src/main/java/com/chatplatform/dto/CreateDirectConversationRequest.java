package com.chatplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateDirectConversationRequest {
    @NotNull(message = "Target user ID is required")
    @NotBlank(message = "Target user ID cannot be blank")
    private String targetUserId;
    
    // Constructors
    public CreateDirectConversationRequest() {}
    
    public CreateDirectConversationRequest(String targetUserId) {
        this.targetUserId = targetUserId;
    }
    
    // Getters and setters
    public String getTargetUserId() {
        return targetUserId;
    }
    
    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }
}
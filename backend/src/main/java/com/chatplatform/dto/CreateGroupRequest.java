package com.chatplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;

public class CreateGroupRequest {
    
    @NotBlank(message = "Group name is required")
    @Size(max = 100, message = "Group name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Group description cannot exceed 500 characters")
    private String description;
    
    private Boolean isPublic = false;
    
    @Min(value = 2, message = "Group must have at least 2 participants")
    @Max(value = 1000, message = "Group cannot have more than 1000 participants")
    private Integer maxParticipants = 100;
    
    private List<String> participantIds;
    
    public CreateGroupRequest() {}
    
    public CreateGroupRequest(String name, String description, Boolean isPublic, Integer maxParticipants, List<String> participantIds) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.maxParticipants = maxParticipants;
        this.participantIds = participantIds;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public List<String> getParticipantIds() {
        return participantIds;
    }
    
    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }
}
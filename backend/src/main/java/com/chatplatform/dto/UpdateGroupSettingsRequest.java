package com.chatplatform.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class UpdateGroupSettingsRequest {
    
    @Size(max = 100, message = "Group name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Group description cannot exceed 500 characters")
    private String description;
    
    private Boolean isPublic;
    
    @Min(value = 2, message = "Group must have at least 2 participants")
    @Max(value = 1000, message = "Group cannot have more than 1000 participants")
    private Integer maxParticipants;
    
    public UpdateGroupSettingsRequest() {}
    
    public UpdateGroupSettingsRequest(String name, String description, Boolean isPublic, Integer maxParticipants) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.maxParticipants = maxParticipants;
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
}
package com.chatplatform.dto;

import java.time.Instant;

public class UserDto {
    private String id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private boolean isOnline;
    private Instant lastSeenAt;
    
    // Constructors
    public UserDto() {}
    
    public UserDto(String id, String username, String email, String displayName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.isOnline = false;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public void setOnline(boolean online) {
        isOnline = online;
    }
    
    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
    
    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
    
    // Helper methods
    public String getDisplayNameOrUsername() {
        return displayName != null && !displayName.trim().isEmpty() ? displayName : username;
    }
    
    public String getInitials() {
        String name = getDisplayNameOrUsername();
        if (name == null || name.trim().isEmpty()) {
            return "U";
        }
        
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return String.valueOf(name.charAt(0)).toUpperCase();
        }
    }
}
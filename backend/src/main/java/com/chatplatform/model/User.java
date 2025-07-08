package com.chatplatform.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    private String id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String displayName;
    private String avatarUrl;
    private Instant createdAt;
    private Instant lastSeenAt;
    private boolean isOnline;
    
    public User() {
        this.createdAt = Instant.now();
    }
    
    public User(String id, String username, String email, String password, String displayName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
        this.createdAt = Instant.now();
        this.isOnline = false;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean isAccountNonExpired() { 
        return true; 
    }
    
    @Override
    public boolean isAccountNonLocked() { 
        return true; 
    }
    
    @Override
    public boolean isCredentialsNonExpired() { 
        return true; 
    }
    
    @Override
    public boolean isEnabled() { 
        return true; 
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
    
    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
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
    
    public Instant getCreatedAt() { 
        return createdAt; 
    }
    
    public void setCreatedAt(Instant createdAt) { 
        this.createdAt = createdAt; 
    }
    
    public Instant getLastSeenAt() { 
        return lastSeenAt; 
    }
    
    public void setLastSeenAt(Instant lastSeenAt) { 
        this.lastSeenAt = lastSeenAt; 
    }
    
    public boolean isOnline() { 
        return isOnline; 
    }
    
    public void setOnline(boolean online) { 
        isOnline = online; 
    }
}
package com.chatplatform.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class ConnectionManager {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public ConnectionManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public void registerConnection(String userId, String serverId, String sessionId) {
        redisTemplate.opsForValue().set(
            "user:server:" + userId, 
            serverId, 
            Duration.ofHours(24)
        );
        
        redisTemplate.opsForSet().add("server:sessions:" + serverId, sessionId);
        
        redisTemplate.opsForValue().set(
            "user:presence:" + userId, 
            "online", 
            Duration.ofMinutes(5)
        );
    }
    
    public void unregisterConnection(String userId, String sessionId) {
        String serverId = getServerId();
        
        redisTemplate.delete("user:server:" + userId);
        redisTemplate.opsForSet().remove("server:sessions:" + serverId, sessionId);
        redisTemplate.opsForValue().set(
            "user:presence:" + userId, 
            "offline", 
            Duration.ofMinutes(1)
        );
    }
    
    public String getServerForUser(String userId) {
        return redisTemplate.opsForValue().get("user:server:" + userId);
    }
    
    public Set<String> getSessionsForServer(String serverId) {
        return redisTemplate.opsForSet().members("server:sessions:" + serverId);
    }
    
    public String getUserPresence(String userId) {
        String presence = redisTemplate.opsForValue().get("user:presence:" + userId);
        return presence != null ? presence : "offline";
    }
    
    public boolean isUserOnline(String userId) {
        return "online".equals(getUserPresence(userId));
    }
    
    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
}
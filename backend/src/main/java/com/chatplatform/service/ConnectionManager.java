package com.chatplatform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final RedisTemplate<String, String> redisTemplate;

    // In-memory fallback: serverId → set of sessionIds
    // Primary source of truth when Redis is unavailable (single-instance deployment)
    private final ConcurrentHashMap<String, Set<String>> localSessions = new ConcurrentHashMap<>();

    public ConnectionManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void registerConnection(String userId, String serverId, String sessionId) {
        // Always register locally — this is the reliable path
        localSessions.computeIfAbsent(serverId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Best-effort Redis registration for multi-instance scenarios
        try {
            redisTemplate.opsForValue().set("user:server:" + userId, serverId, Duration.ofHours(24));
            redisTemplate.opsForSet().add("server:sessions:" + serverId, sessionId);
            redisTemplate.opsForValue().set("user:presence:" + userId, "online", Duration.ofMinutes(5));
        } catch (Exception e) {
            logger.debug("Redis unavailable for registerConnection, using local tracking only");
        }
    }

    public void unregisterConnection(String userId, String sessionId) {
        String serverId = getServerId();

        // Always clean up locally
        Set<String> localSet = localSessions.get(serverId);
        if (localSet != null) {
            localSet.remove(sessionId);
        }

        // Best-effort Redis cleanup
        try {
            redisTemplate.delete("user:server:" + userId);
            redisTemplate.opsForSet().remove("server:sessions:" + serverId, sessionId);
            redisTemplate.opsForValue().set("user:presence:" + userId, "offline", Duration.ofMinutes(1));
        } catch (Exception e) {
            logger.debug("Redis unavailable for unregisterConnection, local cleanup done");
        }
    }

    public String getServerForUser(String userId) {
        try {
            return redisTemplate.opsForValue().get("user:server:" + userId);
        } catch (Exception e) {
            logger.debug("Redis unavailable for getServerForUser");
            return null;
        }
    }

    public Set<String> getSessionsForServer(String serverId) {
        // Try Redis first (needed for multi-instance deployments)
        try {
            Set<String> redisSessions = redisTemplate.opsForSet().members("server:sessions:" + serverId);
            if (redisSessions != null && !redisSessions.isEmpty()) {
                return redisSessions;
            }
        } catch (Exception e) {
            logger.debug("Redis unavailable for getSessionsForServer, falling back to local");
        }

        // Fall back to local in-memory map
        Set<String> local = localSessions.get(serverId);
        return local != null ? local : Collections.emptySet();
    }

    public String getUserPresence(String userId) {
        try {
            String presence = redisTemplate.opsForValue().get("user:presence:" + userId);
            return presence != null ? presence : "offline";
        } catch (Exception e) {
            logger.debug("Redis unavailable for getUserPresence");
            return "offline";
        }
    }

    public boolean isUserOnline(String userId) {
        return "online".equals(getUserPresence(userId));
    }

    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
}

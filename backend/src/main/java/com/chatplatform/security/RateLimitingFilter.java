package com.chatplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter to reduce abuse (DDoS, brute force). Tracks separate sliding windows per
 * limit category so aggressive /api/health polling does not starve unrelated API quotas.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Login / registration / forgot-password bursts */
    private static final int AUTH_REQUESTS_PER_MINUTE = 5;
    /** General authenticated API usage */
    private static final int API_REQUESTS_PER_MINUTE = 100;
    /** WebSocket connect attempts */
    private static final int WEBSOCKET_CONNECTIONS_PER_MINUTE = 10;
    /** Deep/actuator probes — shields PostgreSQL / MongoDB / Redis from high-QPS scripted abuse */
    private static final int HEALTH_REQUESTS_PER_MINUTE = 30;

    /** Sliding window (1 minute) */
    private static final long WINDOW_DURATION = 60 * 1000;

    enum LimitBucket {
        HEALTH,
        AUTH,
        WEBSOCKET,
        API
    }

    private final Map<LimitBucket, Map<String, RequestCounter>> buckets = new EnumMap<>(LimitBucket.class);

    public RateLimitingFilter() {
        for (LimitBucket b : LimitBucket.values()) {
            buckets.put(b, new ConcurrentHashMap<>());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        LimitBucket resolved = resolveBucket(request.getRequestURI());
        if (resolved == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = limitForBucket(resolved);
        String clientIp = getClientIpAddress(request);

        if (limit > 0 && isRateLimitExceeded(clientIp, resolved, limit)) {
            logger.warn("⚠️ Rate limit exceeded (bucket={}, ip={})", resolved, sanitizeIpForLog(clientIp));

            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Avoid logging rare IPv6 in full verbosity at WARN level abuse cases */
    private static String sanitizeIpForLog(String ip) {
        if (ip != null && ip.length() > 45) {
            return ip.substring(0, 45) + "...";
        }
        return ip;
    }

    private LimitBucket resolveBucket(String requestPath) {
        if ("/health".equals(requestPath)) {
            return LimitBucket.HEALTH;
        }
        if (requestPath.startsWith("/ws/")) {
            return LimitBucket.WEBSOCKET;
        }
        // Health probes (heavy DB/redis/mongo work or Spring health aggregation)
        if (requestPath.startsWith("/api/health/")
                || requestPath.startsWith("/api/actuator/health")) {
            return LimitBucket.HEALTH;
        }
        if (requestPath.startsWith("/actuator/health")) {
            return LimitBucket.HEALTH;
        }
        // Auth flows (explicit prefix before generic /api/)
        if (requestPath.startsWith("/api/auth/")) {
            return LimitBucket.AUTH;
        }
        if (requestPath.startsWith("/api/")) {
            return LimitBucket.API;
        }
        return null;
    }

    private int limitForBucket(LimitBucket bucket) {
        return switch (bucket) {
            case AUTH -> AUTH_REQUESTS_PER_MINUTE;
            case HEALTH -> HEALTH_REQUESTS_PER_MINUTE;
            case WEBSOCKET -> WEBSOCKET_CONNECTIONS_PER_MINUTE;
            case API -> API_REQUESTS_PER_MINUTE;
        };
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private boolean isRateLimitExceeded(String clientIp, LimitBucket bucket, int rateLimit) {
        long currentTime = Instant.now().toEpochMilli();
        Map<String, RequestCounter> bucketMap = buckets.get(bucket);

        RequestCounter counter = bucketMap.computeIfAbsent(clientIp, k -> new RequestCounter());

        synchronized (counter) {
            counter.timestamps.entrySet().removeIf(entry ->
                currentTime - entry.getKey() * 1000L > WINDOW_DURATION
            );

            int currentRequests = counter.timestamps.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();

            if (currentRequests >= rateLimit) {
                return true;
            }

            long timeSlot = currentTime / 1000;
            counter.timestamps.computeIfAbsent(timeSlot, k -> new AtomicInteger(0)).incrementAndGet();

            return false;
        }
    }

    /**
     * Request counter per IP inside one {@link LimitBucket}.
     */
    private static class RequestCounter {
        private final Map<Long, AtomicInteger> timestamps = new ConcurrentHashMap<>();
    }

    /**
     * Cleanup old counters for all buckets to limit memory growth.
     */
    public void cleanupOldEntries() {
        long currentTime = Instant.now().toEpochMilli();

        for (Map<String, RequestCounter> bucketMap : buckets.values()) {
            bucketMap.entrySet().removeIf(entry -> {
                RequestCounter counter = entry.getValue();
                synchronized (counter) {
                    counter.timestamps.entrySet().removeIf(timeEntry ->
                        currentTime - timeEntry.getKey() * 1000L > WINDOW_DURATION * 2
                    );
                    return counter.timestamps.isEmpty();
                }
            });
        }

        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            buckets.forEach((k, v) -> sb.append(k).append('=').append(v.size()).append(' '));
            logger.debug("Rate limit buckets after cleanup: {}", sb.toString().trim());
        }
    }
}

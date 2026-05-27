package com.chatplatform.controller;

import com.chatplatform.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    public static final String ENABLED = "enabled";
    public static final String HEADER_DEEP_PROBE_CACHE = "X-Health-Deep-Probe-Cache";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * When both TTLs are &lt;= 0, deep checks run on every request (used in tests with plain Mockito).
     */
    @Value("${app.health.deep-check-cache-when-up-seconds:60}")
    private int cacheTtlUpSeconds;

    /** Shorter cache while dependencies are unhealthy so recovery is noticed quickly. */
    @Value("${app.health.deep-check-cache-when-down-seconds:15}")
    private int cacheTtlDownSeconds;

    private final Object deepHealthLock = new Object();
    private volatile DeepHealthCachedSnapshot deepCheckCache;

    /** Cached deep dependency probe (single JVM instance; not shared across scaled replicas). */
    private record DeepHealthCachedSnapshot(Map<String, Object> body, HttpStatus httpStatus, long expiryEpochMillis) {}

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean cachingEnabled = cacheTtlUpSeconds > 0 || cacheTtlDownSeconds > 0;
        long nowMillis = System.currentTimeMillis();

        DeepHealthCachedSnapshot cached = cachingEnabled ? deepCheckCache : null;
        if (cached != null && nowMillis < cached.expiryEpochMillis()) {
            return respondSnapshot(copiedBody(cached), cached.httpStatus(), "HIT", cachingEnabled);
        }

        if (!cachingEnabled) {
            DeepHealthComputationResult fresh = computeDeepHealth();
            return respondSnapshot(copyNestedStringObjectMap(fresh.body()),
                fresh.httpStatus(), "DISABLED", false);
        }

        synchronized (deepHealthLock) {
            cached = deepCheckCache;
            nowMillis = System.currentTimeMillis();
            if (cached != null && nowMillis < cached.expiryEpochMillis()) {
                return respondSnapshot(copiedBody(cached), cached.httpStatus(), "HIT", true);
            }

            DeepHealthComputationResult fresh = computeDeepHealth();

            logger.info("{} Health probe (deep): PostgreSQL={}, MongoDB={}, Redis={}",
                fresh.allHealthy() ? "✅" : "❌",
                fresh.dependencies().get("postgresql") != null
                    ? ((Map<?, ?>) fresh.dependencies().get("postgresql")).get(Constants.STATUS) : "?",
                fresh.dependencies().get("mongodb") != null
                    ? ((Map<?, ?>) fresh.dependencies().get("mongodb")).get(Constants.STATUS) : "?",
                fresh.dependencies().get("redis") != null
                    ? ((Map<?, ?>) fresh.dependencies().get("redis")).get(Constants.STATUS) : "?");

            int ttlSec = fresh.allHealthy() ? cacheTtlUpSeconds : cacheTtlDownSeconds;
            if (ttlSec <= 0) {
                deepCheckCache = null;
                return respondSnapshot(copyNestedStringObjectMap(fresh.body()),
                    fresh.httpStatus(), null, false);
            }

            long ttlMs = Math.max(ttlSec, 1) * 1000L;
            long expiryEpochMillis = System.currentTimeMillis() + ttlMs;
            DeepHealthCachedSnapshot snapshot = new DeepHealthCachedSnapshot(
                copyNestedStringObjectMap(fresh.body()), fresh.httpStatus(), expiryEpochMillis);
            deepCheckCache = snapshot;
            return respondSnapshot(copiedBody(snapshot), snapshot.httpStatus(), "MISS", true);
        }
    }

    private ResponseEntity<Map<String, Object>> respondSnapshot(Map<String, Object> body, HttpStatus status,
                                                               String probeCacheHdr, boolean addHeaderIfDeep) {
        HttpHeaders headers = new HttpHeaders();
        if (addHeaderIfDeep && probeCacheHdr != null) {
            headers.add(HEADER_DEEP_PROBE_CACHE, probeCacheHdr);
        }
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    /** Defensive clone from cache snapshot (immutable snapshot already uses copied maps — double-safe). */
    private static Map<String, Object> copiedBody(DeepHealthCachedSnapshot snap) {
        return copyNestedStringObjectMap(snap.body());
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> copyNestedStringObjectMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), copyNestedStringObjectMap((Map<String, Object>) nested));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    private DeepHealthComputationResult computeDeepHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> dependencies = new HashMap<>();

        health.put("service", "chat-platform-backend");
        health.put("version", "1.0.0");
        health.put("timestamp", Instant.now());

        boolean allHealthy = true;

        Map<String, Object> postgres = checkPostgreSQL();
        dependencies.put("postgresql", postgres);
        if (!Constants.UP.equals(postgres.get(Constants.STATUS))) {
            allHealthy = false;
        }

        Map<String, Object> mongodb = checkMongoDB();
        dependencies.put("mongodb", mongodb);
        if (!Constants.UP.equals(mongodb.get(Constants.STATUS))) {
            allHealthy = false;
        }

        Map<String, Object> redis = checkRedis();
        dependencies.put("redis", redis);
        if (!Constants.UP.equals(redis.get(Constants.STATUS))) {
            allHealthy = false;
        }

        health.put("dependencies", dependencies);
        health.put(Constants.STATUS, allHealthy ? Constants.UP : Constants.DOWN);

        HttpStatus httpStatus = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return new DeepHealthComputationResult(health, dependencies, allHealthy, httpStatus);
    }

    /** Flatten nested structure for cloning (computes deps map already inside health map). */
    private record DeepHealthComputationResult(Map<String, Object> body, Map<String, Object> dependencies,
                                               boolean allHealthy, HttpStatus httpStatus) {}

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        logger.debug("📋 Health test endpoint called");
        Map<String, Object> body = new HashMap<>();
        body.put(Constants.MESSAGE, Constants.BACKEND_IS_WORKING_CORRECTLY);
        body.put("timestamp", Instant.now());
        body.put("features", Map.of(
            "authentication", ENABLED,
            "websockets", ENABLED,
            "messaging", ENABLED,
            "persistence", ENABLED));
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> checkPostgreSQL() {
        Map<String, Object> postgres = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                long responseTime = System.currentTimeMillis() - startTime;

                postgres.put(Constants.STATUS, isValid ? Constants.UP : Constants.DOWN);
                postgres.put(Constants.RESPONSE_TIME, responseTime + "ms");
                postgres.put("database", connection.getMetaData().getDatabaseProductName());
                postgres.put("url", connection.getMetaData().getURL().replaceAll("password=[^&]*", "password=***"));

                if (isValid) {
                    postgres.put(Constants.DETAILS, "Connection successful");
                } else {
                    postgres.put(Constants.DETAILS, "Connection invalid");
                }
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            postgres.put(Constants.STATUS, Constants.DOWN);
            postgres.put(Constants.RESPONSE_TIME, responseTime + "ms");
            postgres.put(Constants.ERROR, e.getMessage());
            postgres.put(Constants.DETAILS, "Connection failed: " + e.getClass().getSimpleName());
            logger.error("PostgreSQL health check failed", e);
        }

        return postgres;
    }

    private Map<String, Object> checkMongoDB() {
        Map<String, Object> mongodb = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            mongoTemplate.execute(db -> {
                db.runCommand(new org.bson.Document("ping", 1));
                return "pong";
            });

            long responseTime = System.currentTimeMillis() - startTime;
            mongodb.put(Constants.STATUS, Constants.UP);
            mongodb.put(Constants.RESPONSE_TIME, responseTime + "ms");
            mongodb.put("database", mongoTemplate.getDb().getName());
            mongodb.put(Constants.DETAILS, "Ping successful");

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            mongodb.put(Constants.STATUS, Constants.DOWN);
            mongodb.put(Constants.RESPONSE_TIME, responseTime + "ms");
            mongodb.put(Constants.ERROR, e.getMessage());
            mongodb.put(Constants.DETAILS, "Ping failed: " + e.getClass().getSimpleName());
            logger.error("MongoDB health check failed", e);
        }

        return mongodb;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> redisResult = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            long responseTime = System.currentTimeMillis() - startTime;

            redisResult.put(Constants.STATUS, "PONG".equals(pong) ? Constants.UP : Constants.DOWN);
            redisResult.put(Constants.RESPONSE_TIME, responseTime + "ms");
            redisResult.put("response", pong);
            redisResult.put(Constants.DETAILS, "Ping successful");

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            redisResult.put(Constants.STATUS, Constants.DOWN);
            redisResult.put(Constants.RESPONSE_TIME, responseTime + "ms");
            redisResult.put(Constants.ERROR, e.getMessage());
            redisResult.put(Constants.DETAILS, "Ping failed: " + e.getClass().getSimpleName());
            logger.error("Redis health check failed", e);
        }

        return redisResult;
    }
}

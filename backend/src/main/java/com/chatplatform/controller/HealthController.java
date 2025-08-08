package com.chatplatform.controller;

import com.chatplatform.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    public static final String ENABLED = "enabled";
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        logger.info("Comprehensive health check requested");
        
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> dependencies = new HashMap<>();
        
        // Overall service info
        health.put("service", "chat-platform-backend");
        health.put("version", "1.0.0");
        health.put("timestamp", Instant.now());
        
        boolean allHealthy = true;
        
        // Check PostgreSQL
        Map<String, Object> postgres = checkPostgreSQL();
        dependencies.put("postgresql", postgres);
        if (!Constants.UP.equals(postgres.get(Constants.STATUS))) {
            allHealthy = false;
        }
        
        // Check MongoDB
        Map<String, Object> mongodb = checkMongoDB();
        dependencies.put("mongodb", mongodb);
        if (!Constants.UP.equals(mongodb.get(Constants.STATUS))) {
            allHealthy = false;
        }
        
        // Check Redis
        Map<String, Object> redis = checkRedis();
        dependencies.put("redis", redis);
        if (!Constants.UP.equals(redis.get(Constants.STATUS))) {
            allHealthy = false;
        }
        
        health.put("dependencies", dependencies);
        health.put(Constants.STATUS, allHealthy ? Constants.UP : Constants.DOWN);
        
        // Return appropriate HTTP status
        HttpStatus status = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        
        logger.info("Health check completed: {} (PostgreSQL: {}, MongoDB: {}, Redis: {})", 
            allHealthy ? "HEALTHY" : "UNHEALTHY",
            postgres.get("status"), mongodb.get("status"), redis.get("status"));
            
        return ResponseEntity.status(status).body(health);
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        logger.info("Test endpoint called");
        return ResponseEntity.ok(Map.of(
            Constants.MESSAGE, Constants.BACKEND_IS_WORKING_CORRECTLY,
            "timestamp", Instant.now(),
            "features", Map.of(
                "authentication", ENABLED,
                "websockets", ENABLED,
                "messaging", ENABLED,
                "persistence", ENABLED
            )
        ));
    }
    
    private Map<String, Object> checkPostgreSQL() {
        Map<String, Object> postgres = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 second timeout
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
            // Try to execute a simple command
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
        Map<String, Object> redis = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // Try to ping Redis
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            long responseTime = System.currentTimeMillis() - startTime;
            
            redis.put(Constants.STATUS, "PONG".equals(pong) ? Constants.UP : Constants.DOWN);
            redis.put(Constants.RESPONSE_TIME, responseTime + "ms");
            redis.put("response", pong);
            redis.put(Constants.DETAILS, "Ping successful");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            redis.put(Constants.STATUS, Constants.DOWN);
            redis.put(Constants.RESPONSE_TIME, responseTime + "ms");
            redis.put(Constants.ERROR, e.getMessage());
            redis.put(Constants.DETAILS, "Ping failed: " + e.getClass().getSimpleName());
            logger.error("Redis health check failed", e);
        }
        
        return redis;
    }
}
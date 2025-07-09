package com.chatplatform.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    public static final String ENABLED = "enabled";

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        logger.info("Health check requested");
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now(),
            "service", "chat-platform-backend",
            "version", "1.0.0"
        ));
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        logger.info("Test endpoint called");
        return ResponseEntity.ok(Map.of(
            "message", "Backend is working correctly",
            "timestamp", Instant.now(),
            "features", Map.of(
                "authentication", ENABLED,
                "websockets", ENABLED,
                "messaging", ENABLED,
                "persistence", ENABLED
            )
        ));
    }
}
package com.chatplatform.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Template for future integration tests using Testcontainers
 * 
 * To enable:
 * 1. Remove @Disabled annotation
 * 2. Add your actual test methods
 * 3. Configure application properties as needed
 * 
 * This will spin up real Docker containers for testing
 */
@Disabled("Template for future integration tests")
@SpringBootTest
@Testcontainers
public class IntegrationTestTemplate {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2")
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // MongoDB
        registry.add("spring.data.mongodb.uri", () -> 
            "mongodb://" + mongodb.getHost() + ":" + mongodb.getFirstMappedPort() + "/testdb");
        
        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        
        // Disable Kafka for integration tests (or add Kafka container if needed)
        registry.add("spring.kafka.enabled", () -> "false");
    }
    
    @Test
    void contextLoads() {
        // Verify that Spring context loads with real containers
        // Add your integration test logic here
    }
    
    // Example: Test full conversation flow with real databases
    // @Test
    // void testConversationCreationFlow() {
    //     // Test creating conversation, adding participants, sending messages
    //     // This would test the full stack with real databases
    // }
}
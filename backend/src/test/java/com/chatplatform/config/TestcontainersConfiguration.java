package com.chatplatform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
@Profile("test")
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("testchatdb")
                .withUsername("testuser")
                .withPassword("testpass")
                .withReuse(true);
    }

    @Bean
    @ServiceConnection
    public MongoDBContainer mongoContainer() {
        return new MongoDBContainer("mongo:7.0")
                .withReuse(true);
    }

    // Redis, Kafka, and Elasticsearch containers removed for single-service deployment
    // Using in-memory message queue instead of Kafka
    // Using external Redis service for deployment
}
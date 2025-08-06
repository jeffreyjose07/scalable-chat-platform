package com.chatplatform.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        // Setup will be done in individual tests as needed
    }

    @Test
    void testGetStatus_AllHealthy() throws SQLException {
        // Mock PostgreSQL
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        // Mock MongoDB
        when(mongoTemplate.execute(any())).thenReturn("pong");
        when(mongoTemplate.getDb()).thenReturn(mock(com.mongodb.client.MongoDatabase.class));
        when(mongoTemplate.getDb().getName()).thenReturn("testdb");

        // Mock Redis
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        ResponseEntity<?> response = healthController.getStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertEquals("chat-platform-backend", body.get("service"));
        assertEquals("1.0.0", body.get("version"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> dependencies = (Map<String, Object>) body.get("dependencies");
        assertNotNull(dependencies);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> postgresql = (Map<String, Object>) dependencies.get("postgresql");
        assertEquals("UP", postgresql.get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> mongodb = (Map<String, Object>) dependencies.get("mongodb");
        assertEquals("UP", mongodb.get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> redis = (Map<String, Object>) dependencies.get("redis");
        assertEquals("UP", redis.get("status"));
    }

    @Test
    void testGetStatus_PostgreSQLDown() throws SQLException {
        // Mock PostgreSQL failure
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // Mock MongoDB healthy
        when(mongoTemplate.execute(any())).thenReturn("pong");
        when(mongoTemplate.getDb()).thenReturn(mock(com.mongodb.client.MongoDatabase.class));
        when(mongoTemplate.getDb().getName()).thenReturn("testdb");

        // Mock Redis healthy
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        ResponseEntity<?> response = healthController.getStatus();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("DOWN", body.get("status"));
    }

    @Test
    void testGetStatus_MongoDBDown() throws SQLException {
        // Mock PostgreSQL healthy
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        // Mock MongoDB failure
        when(mongoTemplate.execute(any())).thenThrow(new RuntimeException("MongoDB connection failed"));

        // Mock Redis healthy
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        ResponseEntity<?> response = healthController.getStatus();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("DOWN", body.get("status"));
    }

    @Test
    void testGetStatus_RedisDown() throws SQLException {
        // Mock PostgreSQL healthy
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        // Mock MongoDB healthy
        when(mongoTemplate.execute(any())).thenReturn("pong");
        when(mongoTemplate.getDb()).thenReturn(mock(com.mongodb.client.MongoDatabase.class));
        when(mongoTemplate.getDb().getName()).thenReturn("testdb");

        // Mock Redis failure
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenThrow(new RuntimeException("Redis connection failed"));

        ResponseEntity<?> response = healthController.getStatus();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("DOWN", body.get("status"));
    }

    @Test
    void testGetStatus_InvalidConnection() throws SQLException {
        // Mock PostgreSQL with invalid connection
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(false);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");

        // Mock MongoDB healthy
        when(mongoTemplate.execute(any())).thenReturn("pong");
        when(mongoTemplate.getDb()).thenReturn(mock(com.mongodb.client.MongoDatabase.class));
        when(mongoTemplate.getDb().getName()).thenReturn("testdb");

        // Mock Redis healthy
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        ResponseEntity<?> response = healthController.getStatus();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("DOWN", body.get("status"));
    }

    @Test
    void testTestEndpoint() {
        ResponseEntity<?> response = healthController.test();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Backend is working correctly", body.get("message"));
        assertNotNull(body.get("timestamp"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) body.get("features");
        assertNotNull(features);
        assertEquals("enabled", features.get("authentication"));
        assertEquals("enabled", features.get("websockets"));
        assertEquals("enabled", features.get("messaging"));
        assertEquals("enabled", features.get("persistence"));
    }
}
# Kafka Topic Management

This document explains how the application automatically manages Kafka topics to prevent the "Topic not found" issue.

## Automatic Topic Creation

The application now includes several mechanisms to ensure Kafka topics are always available:

### 1. **KafkaTopicConfig.java**
- Automatically creates the `chat-messages` topic on application startup
- Configures topic with 3 partitions and replication factor of 1
- Uses Spring Boot's `@Bean` annotation for automatic registration

### 2. **KafkaTopicValidator.java**
- Runs on application startup (`CommandLineRunner`)
- Validates that all required topics exist
- Creates missing topics automatically
- Provides detailed logging about topic status

### 3. **KafkaHealthService.java**
- Periodic health checks every 30 seconds
- Monitors Kafka connectivity and topic availability
- Automatically recreates topics if they disappear
- Provides health status information

### 4. **Enhanced MessageService.java**
- Fallback mechanism when Kafka is unavailable
- Direct event publishing if Kafka topic send fails
- Comprehensive error handling and logging
- Retry mechanism with proper timeout handling

## Configuration

### Application Properties
```yaml
spring:
  kafka:
    admin:
      fail-fast: false  # Don't fail startup if Kafka is temporarily unavailable
      properties:
        request.timeout.ms: 30000
        connections.max.idle.ms: 30000
    producer:
      properties:
        retries: 3
        request.timeout.ms: 30000
        delivery.timeout.ms: 60000
```

## Troubleshooting

### If topics still disappear:

1. **Check Kafka logs**: `docker-compose logs kafka`
2. **Verify topic creation**: `docker exec <kafka-container> kafka-topics --list --bootstrap-server localhost:9092`
3. **Check application logs**: Look for `KafkaTopicValidator` and `KafkaHealthService` messages
4. **Manual topic creation**: `docker exec <kafka-container> kafka-topics --create --topic chat-messages --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3`

### Application Behavior

- **Normal operation**: Messages flow through Kafka for distribution
- **Kafka unavailable**: Messages saved to MongoDB, distributed via direct events
- **Topic missing**: Application automatically recreates topics
- **Connection issues**: Comprehensive logging and fallback mechanisms

## Monitoring

The application logs provide detailed information about Kafka health:

```
✅ Kafka topic validation successful
❌ Failed to create or validate 'chat-messages' topic
🔄 Topic 'chat-messages' not found. Creating...
```

## Prevention Measures

1. **Automatic topic creation** on startup
2. **Periodic health checks** every 30 seconds
3. **Fallback messaging** when Kafka fails
4. **Comprehensive error handling** with retries
5. **Proper Kafka configuration** with timeouts and connection management
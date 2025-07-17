# Chat Platform Backend

A comprehensive Spring Boot backend for a scalable chat platform with real-time messaging, group management, and role-based access control.

## 🚀 Features

### Core Functionality
- **Real-time messaging** via WebSocket with automatic reconnection
- **User authentication** with JWT tokens
- **Direct messaging** between users
- **Group conversations** with advanced management
- **Role-based access control** (OWNER/ADMIN/MEMBER)
- **Conversation deletion** with automatic message cleanup
- **Message persistence** with MongoDB
- **Session management** with Redis
- **Message queuing** with Kafka

### Advanced Group Management
- **Group creation** with customizable settings
- **Role-based permissions** with hierarchical access
- **Participant management** (add/remove users)
- **Group settings** (name, description, visibility, limits)
- **Group deletion** with proper permission checks and message cleanup
- **Security controls** with permission validation

### Architecture
- **Event-driven design** with Kafka integration
- **Multi-database strategy** (PostgreSQL + MongoDB + Redis)
- **Microservices-ready** with stateless design
- **Comprehensive logging** with structured output
- **Health monitoring** and diagnostics

## 📋 Requirements

- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for dependencies)

## 🛠️ Quick Start

### 1. Start Dependencies
```bash
# From project root
./start-dev.sh
```

### 2. Run Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Verify Setup
```bash
curl http://localhost:8080/api/health
```

## 🏗️ Project Structure

```
backend/src/main/java/com/chatplatform/
├── config/
│   ├── CorsConfig.java           # CORS configuration
│   ├── JwtTokenUtil.java         # JWT token utilities
│   ├── SecurityConfig.java       # Security configuration
│   └── WebSocketConfig.java      # WebSocket configuration
├── controller/
│   ├── AuthController.java       # Authentication endpoints
│   ├── ConversationController.java # Conversation management
│   ├── MessageController.java    # Message endpoints
│   └── UserController.java       # User management
├── dto/
│   ├── ConversationDto.java      # Conversation data transfer
│   ├── CreateGroupRequest.java   # Group creation payload
│   ├── UpdateGroupSettingsRequest.java # Group settings payload
│   ├── MessageDto.java           # Message data transfer
│   └── UserDto.java              # User data transfer
├── model/
│   ├── Conversation.java         # Conversation entity
│   ├── ConversationParticipant.java # Participant entity
│   ├── ConversationType.java     # Conversation type enum
│   ├── ParticipantRole.java      # Role enum (OWNER/ADMIN/MEMBER)
│   ├── Message.java              # Message entity
│   └── User.java                 # User entity
├── repository/
│   ├── jpa/                      # JPA repositories
│   │   ├── ConversationRepository.java
│   │   ├── ConversationParticipantRepository.java
│   │   └── UserRepository.java
│   └── mongo/                    # MongoDB repositories
│       └── MessageRepository.java
├── service/
│   ├── ConversationService.java  # Conversation business logic
│   ├── MessageService.java       # Message business logic
│   ├── UserService.java          # User business logic
│   └── WebSocketService.java     # WebSocket management
└── websocket/
    ├── ChatHandler.java          # WebSocket message handler
    └── MessageType.java          # WebSocket message types
```

## 🔐 Security & Permissions

### Role-Based Access Control
The system implements a hierarchical permission model:

#### OWNER (Full Control)
- Create and delete groups (with complete message cleanup)
- Manage all participants (add/remove/promote/demote)
- Update all group settings
- Transfer ownership

#### ADMIN (Management)
- Manage participants (add/remove)
- Update group settings (name, description, visibility)
- Cannot delete group or manage other admins

#### MEMBER (Basic Access)
- Send and receive messages
- View conversation history
- Leave group

### Permission Validation
All endpoints validate permissions before executing operations:

```java
// Example permission check
@PostMapping("/{conversationId}/participants")
public ResponseEntity<Void> addParticipant(
    @PathVariable String conversationId,
    @RequestParam String participantId,
    Authentication authentication) {
    
    String userId = getUserId(authentication);
    
    if (!conversationService.canManageParticipants(userId, conversationId)) {
        return ResponseEntity.status(403).build();
    }
    
    // ... rest of implementation
}
```

## 📊 Database Schema

### PostgreSQL (User Data)
```sql
-- Users table
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar_url VARCHAR(255),
    created_at TIMESTAMP,
    last_seen_at TIMESTAMP,
    is_online BOOLEAN DEFAULT FALSE
);

-- Conversations table
CREATE TABLE conversations (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(255),
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    max_participants INTEGER DEFAULT 100,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Conversation participants table
CREATE TABLE conversation_participants (
    conversation_id VARCHAR(255),
    user_id VARCHAR(255),
    role VARCHAR(50) DEFAULT 'MEMBER',
    joined_at TIMESTAMP,
    last_read_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (conversation_id, user_id)
);
```

### MongoDB (Message Data)
```javascript
// Messages collection
{
  "_id": ObjectId,
  "conversationId": "string",
  "senderId": "string",
  "content": "string",
  "timestamp": ISODate,
  "messageType": "TEXT",
  "isEdited": false,
  "editedAt": ISODate,
  "replyTo": "string"
}
```

## 🔧 Configuration

### Application Properties
```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/chatplatform
spring.data.mongodb.uri=mongodb://localhost:27017/chatplatform
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer

# JWT Configuration
jwt.secret=your-secret-key
jwt.expiration=86400000

# WebSocket Configuration
websocket.allowed-origins=*
```

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=chatplatform
DB_USERNAME=chatuser
DB_PASSWORD=chatpass

# MongoDB
MONGO_URI=mongodb://localhost:27017/chatplatform

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## 🧪 Testing

### Unit Tests
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ConversationServiceTest

# Run tests with coverage
./mvnw test jacoco:report
```

### Integration Tests
```bash
# Run integration tests
./mvnw test -Dtest=*IT

# Run with test containers
./mvnw test -Dspring.profiles.active=test
```

### API Testing
```bash
# Test authentication
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password"}'

# Test group creation
curl -X POST http://localhost:8080/api/conversations/groups \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Group",
    "description": "A test group",
    "isPublic": false,
    "maxParticipants": 50,
    "participantIds": ["user1", "user2"]
  }'
```

## 📈 Performance

### Database Optimization
- **Indexes** on frequently queried fields
- **Connection pooling** for database connections
- **Caching** with Redis for session data
- **Pagination** for large result sets

### WebSocket Optimization
- **Connection pooling** for WebSocket sessions
- **Message batching** for high-frequency updates
- **Heartbeat monitoring** for connection health
- **Graceful reconnection** handling

## 🔍 Monitoring & Logging

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8080/actuator/health/db

# Kafka connectivity
curl http://localhost:8080/actuator/health/kafka
```

### Logging Configuration
```yaml
logging:
  level:
    com.chatplatform: INFO
    com.chatplatform.service: DEBUG
    org.springframework.web: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 🚀 Production Deployment

### Docker Build
```bash
# Build image
docker build -t chat-platform-backend .

# Run container
docker run -d \
  --name chat-backend \
  -p 8080:8080 \
  -e DB_HOST=your-db-host \
  -e KAFKA_BOOTSTRAP_SERVERS=your-kafka-host:9092 \
  chat-platform-backend
```

### Production Considerations
- **SSL/TLS** configuration for HTTPS
- **Database connection pooling** optimization
- **Load balancing** with multiple instances
- **Caching strategy** optimization
- **Security hardening** and audit logging
- **Monitoring** and alerting setup

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add comprehensive JavaDoc for public methods
- Write unit tests for new functionality
- Follow Spring Boot best practices

## 📝 License

This project is for educational and demonstration purposes.
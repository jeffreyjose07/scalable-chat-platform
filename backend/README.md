# Chat Platform Backend

Spring Boot 3.2 backend with Java 17, optimized for single-service deployment.

## Architecture

### Core Technologies
- **Spring Boot 3.2**: Main application framework
- **Java 17**: Programming language with latest features
- **PostgreSQL**: User accounts and conversation metadata
- **MongoDB**: Chat messages and history
- **Redis**: Session management and caching
- **WebSocket**: Real-time messaging
- **JWT**: Stateless authentication

### Message Flow
```
User Message → WebSocket → MessageService → MongoDB + In-Memory Queue → Distribution → All Connected Users
```

## Key Features

### Authentication & Security
- JWT-based authentication with configurable expiration
- Role-based access control (OWNER/ADMIN/MEMBER)
- WebSocket authentication via interceptors
- Rate limiting and security filters
- CORS configuration for cross-origin requests

### Real-time Messaging
- WebSocket-based bidirectional communication
- In-memory message queue for fast distribution
- Automatic connection management and session tracking
- Message delivery status and read receipts
- Connection heartbeat and automatic reconnection

### Data Management
- Multi-database strategy for optimal performance
- PostgreSQL for structured user/conversation data
- MongoDB for flexible message storage with indexing
- Redis for fast session and cache management
- Soft delete for conversations with proper cleanup

## Project Structure

```
src/main/java/com/chatplatform/
├── config/                  # Spring configuration classes
│   ├── SecurityConfig.java  # JWT and security setup
│   ├── WebSocketConfig.java # WebSocket configuration
│   └── RedisConfig.java     # Redis connection setup
├── controller/              # REST API endpoints
│   ├── AuthController.java  # Authentication endpoints
│   ├── MessageController.java # Message API
│   └── ConversationController.java # Conversation management
├── dto/                     # Data transfer objects
├── model/                   # Entity models (JPA + MongoDB)
│   ├── User.java            # User entity (PostgreSQL)
│   ├── Conversation.java    # Conversation entity (PostgreSQL)
│   └── ChatMessage.java     # Message entity (MongoDB)
├── repository/              # Data access layer
│   ├── jpa/                 # PostgreSQL repositories
│   └── mongo/               # MongoDB repositories
├── service/                 # Business logic
│   ├── MessageService.java  # Message processing and distribution
│   ├── AuthService.java     # Authentication logic
│   └── ConversationService.java # Conversation management
└── websocket/               # WebSocket handling
    └── ChatWebSocketHandler.java # Real-time message handling
```

## Build & Deployment

### Local Development
```bash
# Start infrastructure (PostgreSQL, MongoDB, Redis)
../start-dev.sh

# Run application
export JAVA_HOME=/path/to/java17
./gradlew bootRun
```

### Production Build
```bash
# Build with embedded frontend
export JAVA_HOME=/path/to/java17
./gradlew buildForRender
```

### Build Optimizations
- **Lazy initialization**: `spring.main.lazy-initialization: true`
- **JVM tuning**: `-XX:TieredStopAtLevel=1 -noverify`
- **Gradle caching**: Parallel builds and dependency caching
- **Docker optimization**: Multi-stage builds with layer caching

## Configuration

### Environment Profiles
- **local**: Development with localhost databases
- **docker**: Docker Compose with container networking
- **render**: Production deployment with external services

### Key Configuration Properties
```yaml
# Database connections
spring.datasource.url: PostgreSQL connection
spring.data.mongodb.uri: MongoDB connection  
spring.redis.host: Redis connection

# JWT configuration
app.jwt.secret: JWT signing key
app.jwt.expiration: Token expiration time

# CORS settings
app.cors.allowed-origins: Allowed frontend origins
```

## API Documentation

### Authentication Endpoints
```
POST /api/auth/login     # User login
POST /api/auth/register  # User registration  
POST /api/auth/logout    # User logout
```

### Message Endpoints
```
GET  /api/messages/{conversationId}  # Get conversation messages
POST /api/messages                   # Send message
```

### Conversation Endpoints
```
GET    /api/conversations           # Get user conversations
POST   /api/conversations/direct    # Create direct conversation
POST   /api/conversations/groups    # Create group conversation
DELETE /api/conversations/{id}      # Delete conversation
```

### WebSocket Endpoint
```
/ws/chat  # Real-time messaging with JWT authentication
```

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests
```bash
./gradlew integrationTest
```

### Test Coverage
```bash
./gradlew localAnalysis  # Generate JaCoCo coverage report
```

## Performance Features

### Startup Optimization
- Lazy initialization reduces startup time by 20-40%
- JVM flags optimized for fast startup in containers
- Minimal auto-configuration for reduced overhead

### Runtime Performance
- In-memory message queue for sub-100ms message delivery
- Connection pooling for database efficiency
- Redis caching for frequently accessed data
- WebSocket connection reuse and heartbeat management

### Scalability Considerations
- Stateless service design for horizontal scaling
- Database connection pooling with configurable limits
- Session externalization via Redis
- Event-driven architecture with in-memory queuing

## Security Features

### Authentication
- JWT tokens with configurable expiration
- Secure password hashing with BCrypt
- Token blacklist for logout functionality

### Authorization
- Role-based access control for group operations
- Conversation-level permissions
- WebSocket connection authentication

### Data Protection
- Input validation and sanitization
- SQL injection prevention via JPA
- XSS protection headers
- Rate limiting for API endpoints

## Monitoring & Health

### Health Checks
```
GET /api/health/status    # Application health
GET /actuator/health      # Detailed health information
GET /actuator/metrics     # Application metrics
```

### Logging
- Structured logging with correlation IDs
- Configurable log levels per package
- Request/response logging for debugging
- Error tracking and alerting

## Troubleshooting

### Common Issues

**Java Version**: Ensure Java 17+ is installed and JAVA_HOME is set
**Database Connection**: Verify PostgreSQL and MongoDB are accessible
**Redis Connection**: Check Redis connectivity for sessions
**WebSocket Issues**: Verify CORS settings and authentication

### Debug Commands
```bash
# Check Java version
java -version

# Test database connections
curl http://localhost:8080/actuator/health

# View application logs
./gradlew bootRun --info
```

### Performance Monitoring
```bash
# Check JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Monitor connection pools
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```
# Development Setup

Complete guide for setting up the Scalable Chat Platform development environment.

## Quick Start Options

### Option 1: Automated Development Setup (Recommended)
```bash
./start-dev.sh
cd backend && mvn spring-boot:run    # Terminal 2
cd frontend && npm install && npm run start:network    # Terminal 3
```

### Option 2: Full Docker Stack
```bash
./start-full-stack.sh
# Access at http://localhost:3000
```

### Option 3: Manual Setup
```bash
docker-compose up -d
cd backend && mvn spring-boot:run
cd frontend && npm install && npm start
```

## Prerequisites

### Required Software
- **Java 17+** - Backend runtime
- **Node.js 18+** - Frontend development
- **Maven 3.8+** - Backend build tool
- **Docker & Docker Compose** - Infrastructure services
- **Git** - Version control

### Verify Installation
```bash
java --version    # Should show Java 17+
node --version    # Should show Node 18+
mvn --version     # Should show Maven 3.8+
docker --version  # Should show Docker
```

## Architecture Overview

The platform consists of multiple services working together:

### Application Services
- **Backend**: Spring Boot REST API + WebSocket server (Port 8080)
- **Frontend**: React TypeScript application (Port 3000)

### Infrastructure Services
- **PostgreSQL** (Port 5432): User accounts and authentication
- **MongoDB** (Port 27017): Chat messages and conversations
- **Redis** (Port 6379): Sessions and caching
- **Kafka** (Port 9092): Message queuing and distribution
- **Elasticsearch** (Port 9200): Message search indexing

## Development Modes

### Mode 1: Hybrid Development (Recommended)
Best for active development with fast refresh cycles.

**Infrastructure**: Docker containers
**Backend**: Local Maven process 
**Frontend**: Local npm development server

```bash
# Terminal 1: Start infrastructure
./start-dev.sh

# Terminal 2: Start backend
cd backend
mvn spring-boot:run

# Terminal 3: Start frontend  
cd frontend
npm install
npm run start:network  # For network access
```

**Benefits:**
- ✅ Fast backend recompilation
- ✅ Hot frontend reloading
- ✅ Full debugging capabilities
- ✅ Network access from other devices

### Mode 2: Full Docker Stack
Best for testing production-like environment.

```bash
./start-full-stack.sh
```

**Benefits:**
- ✅ Production-like environment
- ✅ Single command startup
- ✅ Consistent across systems
- ✅ Easier deployment testing

### Mode 3: Manual Setup
Best for understanding the system architecture.

```bash
# Start infrastructure
docker-compose up -d

# Wait for services
sleep 15

# Create Kafka topics
docker exec scalable-chat-platform-kafka-1 kafka-topics \
  --create --topic chat-messages \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 --partitions 3

# Start backend
cd backend && mvn spring-boot:run

# Start frontend
cd frontend && npm install && npm start
```

## Service Details

### Backend Development
- **Main class**: `com.chatplatform.ChatPlatformApplication`
- **API Base**: `http://localhost:8080`
- **WebSocket**: `ws://localhost:8080/ws/chat`
- **Health Check**: `http://localhost:8080/api/health/status`
- **Profile**: `local` (default for development)

**Key Java 17 Features Used:**
- Records for DTOs and immutable data
- Pattern matching with switch expressions
- Text blocks for SQL and JSON strings
- Enhanced Stream API operations

### Frontend Development
- **Main component**: `src/App.tsx`
- **Development server**: `http://localhost:3000`
- **Network access**: `npm run start:network` for multi-device testing
- **Auto-reload**: On file changes
- **Debug tools**: Network Info button in chat interface

**Environment Detection:**
```typescript
// Automatic API URL detection
REACT_APP_API_URL=http://localhost:8080          // Local development
REACT_APP_API_URL=http://YOUR_IP:8080           // Network access
REACT_APP_WS_URL=ws://localhost:8080/ws/chat    // WebSocket endpoint
```

### Database Access

#### PostgreSQL (User Data)
```bash
Host: localhost:5432
Database: chatdb  
Username: chatuser
Password: chatpass
```

#### MongoDB (Messages)
```bash
Host: localhost:27017
Database: chatdb
# No authentication for development
```

#### Redis (Sessions)
```bash
Host: localhost:6379
Port: 6379
# No authentication for development
```

## Testing the Application

### Basic Functionality Test
1. **Registration**: Use any email/password (demo mode enabled)
2. **Login**: Access with created credentials
3. **Messaging**: Send messages in different conversations
4. **Real-time**: Open multiple browser tabs to test live updates
5. **Persistence**: Stop/start services to verify data retention

### Multi-Device Testing
```bash
# Terminal 1: Start with network access
cd frontend && npm run start:network

# Terminal 2: Start second instance
cd frontend && PORT=3001 npm run start:network

# Access points:
# http://localhost:3000
# http://localhost:3001
# http://YOUR_IP:3000 (from other devices)
```

### API Testing
Key endpoints to verify:
- `POST /api/auth/login` - Authentication
- `GET /api/auth/me` - Current user info
- `GET /api/conversations` - User conversations
- `POST /api/conversations` - Create conversation
- `WebSocket: ws://localhost:8080/ws/chat?token=<jwt>`

## Troubleshooting

### Common Issues

#### Kafka Topic Issues
**Symptoms**: Messages not appearing in real-time
**Solution**:
```bash
# Check topic exists
docker exec scalable-chat-platform-kafka-1 kafka-topics \
  --list --bootstrap-server localhost:9092

# Recreate if missing
./start-dev.sh
```

#### Kafka Cluster ID Mismatch
**Symptoms**: `ClusterAuthorizationException` 
**Solution**:
```bash
# Fix without data loss
./fix-kafka-only.sh

# Nuclear option (loses all data)
docker-compose down -v && ./start-dev.sh
```

#### Port Conflicts
**Symptoms**: `Port already in use`
**Solution**:
```bash
# Kill conflicting processes
sudo lsof -ti:3000 | xargs kill -9
sudo lsof -ti:8080 | xargs kill -9
sudo lsof -ti:5432 | xargs kill -9
```

#### Database Connection Issues
**Symptoms**: Backend fails to start
**Solution**:
```bash
# Check service status
docker-compose ps

# Restart database services
docker-compose restart postgres mongodb redis

# Check logs
docker-compose logs postgres
```

#### WebSocket Connection Failures
**Symptoms**: Real-time features not working
**Solution**:
1. Check browser console for WebSocket errors
2. Verify CORS configuration allows your origin
3. Ensure Redis is running for session management
4. Check backend logs for connection errors

### Debug Tools

#### Backend Logs
Look for these emoji indicators:
```
✅ Kafka topic validation successful
🚀 Started in-memory message processor  
💾 Message saved to database
📨 Received message from Kafka
🔌 WebSocket connected
❌ Error indicators
```

#### Frontend Debug
- **Network Info Button**: Shows current IP detection and API configuration
- **Browser Console**: WebSocket status and connection errors
- **React DevTools**: Component state inspection
- **Network Tab**: API request/response monitoring

#### Service Health Checks
```bash
# Backend health
curl http://localhost:8080/api/health/status

# Infrastructure status
docker-compose ps

# Service logs
docker-compose logs -f [service-name]
```

## Development Workflow

### Daily Development
```bash
# Start infrastructure
./start-dev.sh

# Start backend in watch mode
cd backend && mvn spring-boot:run

# Start frontend with hot reload
cd frontend && npm run start:network
```

### Clean Shutdown
```bash
# Stop services safely (preserves data)
./stop-dev.sh

# Stop full stack (preserves data)
./stop-full-stack.sh

# Clean shutdown (removes data)
./stop-full-stack.sh --clean
```

### Data Management
```bash
# Preserve data: Use these commands
./stop-dev.sh                    # Saves all data
./fix-kafka-only.sh             # Fixes Kafka without data loss

# Destructive: Avoid unless intentional
docker-compose down -v           # Removes ALL data
./stop-full-stack.sh --purge    # Removes images too
```

## Project Structure

```
scalable-chat-platform/
├── backend/                     # Spring Boot application
│   ├── src/main/java/com/chatplatform/
│   │   ├── config/             # Configuration classes
│   │   ├── controller/         # REST API controllers
│   │   ├── model/             # JPA entities and DTOs
│   │   ├── repository/        # Data access layer
│   │   ├── service/           # Business logic
│   │   └── websocket/         # WebSocket handlers
│   ├── src/main/resources/
│   │   ├── application.yml    # Configuration
│   │   └── static/           # Frontend build output
│   └── pom.xml               # Maven dependencies
├── frontend/                  # React TypeScript app
│   ├── src/
│   │   ├── components/       # Reusable UI components
│   │   ├── hooks/           # Custom React hooks
│   │   ├── pages/           # Page components
│   │   ├── types/           # TypeScript definitions
│   │   └── utils/           # Utility functions
│   ├── package.json         # npm dependencies
│   └── build/              # Production build
├── docker-compose.yml       # Infrastructure services
├── start-dev.sh            # Development startup
├── start-full-stack.sh     # Full Docker startup
└── docs/                   # Documentation
```

## Environment Variables

### Backend Configuration (application.yml)
```yaml
# Profiles: local, docker, render, prod
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  
  # Database connections
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/chatdb}
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/chatdb}
  redis:
    host: ${SPRING_REDIS_HOST:localhost}
    port: ${SPRING_REDIS_PORT:6379}
  
  # Kafka configuration
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

### Frontend Configuration
```bash
# Automatic detection (recommended)
# No manual configuration needed

# Manual override (.env.local)
REACT_APP_API_URL=http://custom-ip:8080
REACT_APP_WS_URL=ws://custom-ip:8080/ws/chat
```

## Performance Tips

### Development Optimization
- Use `npm run start:network` for multi-device testing
- Enable Maven offline mode: `mvn -o spring-boot:run`
- Use Docker build cache for faster container rebuilds
- Monitor resource usage with `docker stats`

### Production Preparation
- Test with production profile: `SPRING_PROFILES_ACTIVE=prod`
- Build optimized frontend: `npm run build`
- Test Docker production build: `./start-full-stack.sh`
- Verify all environment variables are configured

## Next Steps

1. **Set up your IDE**: Configure Java/TypeScript support
2. **Run tests**: Execute unit and integration tests
3. **Explore the API**: Use the health endpoints and WebSocket
4. **Customize features**: Modify components and services
5. **Deploy**: Follow the [deployment guide](../deployment/README.md)

For deployment instructions, see [Deployment Guide](../deployment/README.md).
For contribution guidelines, see [CONTRIBUTING.md](../../CONTRIBUTING.md).
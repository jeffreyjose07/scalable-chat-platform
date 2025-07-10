# Scalable Chat Platform

A real-time chat platform built with Spring Boot backend and React frontend, designed for local development and network access.

## Architecture

- **Backend**: Spring Boot 3.2 with Java 17, WebSocket support, PostgreSQL, MongoDB, Redis, Kafka, Elasticsearch
- **Frontend**: React 18 with TypeScript, WebSocket client, Tailwind CSS
- **Infrastructure**: Docker Compose with persistent volumes and automatic topic management
- **Network Access**: Automatic IP detection and dynamic CORS configuration

📊 **[View System Architecture](docs/ARCHITECTURE.md)** | 🎯 **[Professional Demo Guide](docs/DEMO.md)**

> **Features professional Mermaid diagrams, sequence flows, and comprehensive technical documentation**

## Quick Start

### Prerequisites

- **Docker and Docker Compose** (latest version)
- **Java 17+** (for backend development)
- **Node.js 18+** (for frontend development)
- **Maven 3.8+** (for backend builds)
- **Git** (for version control)

### Local Development Setup

1. **Start Infrastructure Services**
   ```bash
   ./start-dev.sh
   ```
   This starts PostgreSQL, MongoDB, Redis, Kafka, and Elasticsearch with persistent volumes.
   
   ✅ **Automatic Kafka topic creation and persistence**
   ✅ **Zookeeper cluster ID consistency**
   ✅ **Health monitoring and service validation**

2. **Start Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Backend will run on http://localhost:8080
   
   ✅ **Multi-layer topic validation on startup**
   ✅ **Enhanced logging with emojis for debugging**
   ✅ **Automatic failover mechanisms**

3. **Start Frontend**
   ```bash
   cd frontend
   npm install
   npm run start:network  # For network access
   # OR
   npm start              # For localhost only
   ```
   Frontend will run on http://localhost:3000
   
   ✅ **Automatic IP detection and configuration**
   ✅ **Dynamic CORS handling**
   ✅ **Network debug information**

### Testing Real-time Chat

For testing the real-time functionality, start multiple frontend instances:

```bash
# Terminal 1 - First instance
cd frontend && npm start

# Terminal 2 - Second instance  
cd frontend && PORT=3001 npm start

# Terminal 3 - Third instance
cd frontend && PORT=3002 npm start
```

Then open:
- http://localhost:3000 (User 1)
- http://localhost:3001 (User 2) 
- http://localhost:3002 (User 3)

Login with different emails on each instance and start chatting!

### Stop Services

```bash
./stop-dev.sh
```

**Note**: This preserves all data (users, messages, sessions). Only use `docker-compose down -v` if you want to reset everything.

### Network Access

For accessing from other machines on the same network:

```bash
# Start frontend with network access
cd frontend && npm run start:network

# Access from any device on your network
http://YOUR_IP:3000
```

✅ **Automatic IP detection - no hardcoding needed**
✅ **Dynamic CORS configuration**
✅ **Works across different WiFi networks**

## Features

### Core Features
- ✅ **Real-time messaging** via WebSocket with automatic reconnection
- ✅ **Private messaging** with direct conversations and user discovery
- ✅ **Group conversations** with persistent state and participant management
- ✅ **Unread message tracking** with industry-standard timestamp-based system
- ✅ **Message search** within conversations with highlighting and pagination
- ✅ **User authentication** (demo mode with JWT tokens)
- ✅ **Message persistence** (MongoDB with automatic indexing)
- ✅ **Connection management** (Redis with session tracking)
- ✅ **Message queuing** (Kafka with guaranteed delivery)
- ✅ **Responsive UI** with Tailwind CSS and mobile-first design

### Infrastructure Features
- ✅ **Kafka topic auto-creation** and persistence
- ✅ **Zookeeper cluster ID consistency** 
- ✅ **Multi-layer health monitoring**
- ✅ **Automatic IP detection** for network access
- ✅ **Dynamic CORS configuration**
- ✅ **Persistent volumes** for data retention
- ✅ **Enhanced logging** with emoji indicators
- ✅ **Graceful error handling** and recovery

### Technical Highlights
- **Event-Driven Architecture**: Microservices with Kafka message distribution
- **Multi-Database Strategy**: PostgreSQL for users/conversations, MongoDB for messages, Redis for sessions
- **Real-Time Communication**: WebSocket-based instant messaging with unread tracking
- **Modern React Architecture**: Custom hooks pattern without Redux complexity
- **Industry-Standard UX**: Last-read timestamp approach used by Discord/Slack/WhatsApp
- **Horizontal Scalability**: Stateless services designed for load balancing
- **Fault Tolerance**: Message persistence and graceful error handling

## Services and Ports

| Service | Port | Purpose |
|---------|------|---------|
| Backend API | 8080 | Spring Boot application |
| Frontend | 3000 | React development server |
| PostgreSQL | 5432 | User data storage |
| MongoDB | 27017 | Message storage |
| Redis | 6379 | Caching and sessions |
| Kafka | 9092 | Message queuing |
| Elasticsearch | 9200 | Message search (future) |

## Development

### Backend Structure
```
backend/src/main/java/com/chatplatform/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data transfer objects
├── model/          # Entity models
├── repository/     # Data repositories
├── service/        # Business logic
└── websocket/      # WebSocket handlers
```

### Frontend Structure
```
frontend/src/
├── components/     # React components
├── hooks/         # Custom React hooks
├── pages/         # Page components
├── services/      # API services
├── types/         # TypeScript types
└── utils/         # Utility functions
```

## Troubleshooting

### Common Issues

**🔧 Kafka Issues:**
- **Topic missing**: Run `./start-dev.sh` - it will auto-create topics
- **Cluster ID mismatch**: Run `./fix-kafka-only.sh` to fix without data loss
- **Messages not real-time**: Check backend logs for Kafka connection status

**🌐 Network Access:**
- **Login fails from other machines**: Use `npm run start:network` for frontend
- **IP detection wrong**: Check "Network Info" button in chat window
- **CORS errors**: Backend automatically allows private IP ranges

**🐳 Infrastructure:**
- **Port conflicts**: Use `docker ps` to check running containers
- **Services not starting**: Run `docker-compose logs <service-name>`
- **Data loss**: Use `./stop-dev.sh` (preserves data) not `docker-compose down -v`

**💻 Development:**
- **Frontend issues**: Delete `node_modules`, run `npm install`
- **Backend compilation**: Ensure Java 17+ is installed
- **WebSocket connection**: Check browser console for connection errors

### Advanced Troubleshooting

**📊 Health Monitoring:**
```bash
# Check all services
docker-compose ps

# Check specific service logs
docker-compose logs -f kafka

# Verify Kafka topics
docker exec scalable-chat-platform-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
```

**🔍 Debug Network Issues:**
- Use "Network Info" button in chat interface
- Check browser developer tools network tab
- Verify backend accessibility: `curl http://YOUR_IP:8080/api/health`

## API Documentation

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | User authentication |
| GET | `/api/messages/{conversationId}` | Get conversation messages |
| WebSocket | `/ws/chat` | Real-time messaging |

## Testing

The application includes demo authentication - you can login with any email/password combination to test the functionality.

## Production Deployment

For production deployment, additional configuration is needed:
- JWT authentication implementation
- SSL/TLS certificates
- Database connection pooling
- Load balancing
- Monitoring and logging
- Environment-specific configurations

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test locally
5. Submit a pull request

## License

This project is for educational and demonstration purposes.
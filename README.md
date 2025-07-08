# Scalable Chat Platform

A real-time chat platform built with Spring Boot backend and React frontend, designed for local development.

## Architecture

- **Backend**: Spring Boot with WebSocket support, PostgreSQL, MongoDB, Redis, Kafka, Elasticsearch
- **Frontend**: React with TypeScript, WebSocket client, Tailwind CSS
- **Infrastructure**: Docker Compose for local development

📊 **[View System Architecture](docs/ARCHITECTURE.md)** | 🎯 **[Professional Demo Guide](docs/DEMO.md)**

> **Features professional Mermaid diagrams, sequence flows, and comprehensive technical documentation**

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for backend development)
- Node.js 18+ (for frontend development)
- Maven (for backend builds)

### Local Development Setup

1. **Start Infrastructure Services**
   ```bash
   ./start-dev.sh
   ```
   This starts PostgreSQL, MongoDB, Redis, Kafka, and Elasticsearch.

2. **Start Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Backend will run on http://localhost:8080

3. **Start Frontend**
   ```bash
   cd frontend
   npm install
   npm start
   ```
   Frontend will run on http://localhost:3000

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

## Features

### Implemented
- ✅ Real-time messaging via WebSocket
- ✅ Multiple conversation channels
- ✅ User authentication (demo mode)
- ✅ Message persistence (MongoDB)
- ✅ Connection management (Redis)
- ✅ Message queuing (Kafka)
- ✅ Responsive UI with Tailwind CSS

### Technical Highlights
- **Event-Driven Architecture**: Microservices with Kafka message distribution
- **Multi-Database Strategy**: PostgreSQL for users, MongoDB for messages, Redis for sessions
- **Real-Time Communication**: WebSocket-based instant messaging
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

**Port conflicts:**
- If PostgreSQL fails to start, check for existing instances on port 5432
- Stop conflicting services: `docker stop <container-name>`

**Frontend issues:**
- Delete `node_modules` and `package-lock.json`, then run `npm install`
- Ensure `react-scripts` version is 5.0.1 in package.json

**Backend issues:**
- Check if all Docker services are running: `docker-compose ps`
- Verify database connections in application logs

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
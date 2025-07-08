# Development Guide

## Quick Setup

### Option 1: Automated Setup
```bash
./start-dev.sh
```

### Option 2: Manual Setup

1. **Start Infrastructure**
   ```bash
   docker-compose up -d
   ```

2. **Start Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **Start Frontend**
   ```bash
   cd frontend
   npm install
   npm start
   ```

## Development Workflow

### Backend Development
- Main application: `backend/src/main/java/com/chatplatform/ChatPlatformApplication.java`
- Run with: `mvn spring-boot:run`
- API endpoints available at: http://localhost:8080
- WebSocket endpoint: ws://localhost:8080/ws/chat

### Frontend Development
- Main component: `frontend/src/App.tsx`
- Run with: `npm start`
- Available at: http://localhost:3000
- Auto-reloads on file changes

### Testing the Application

1. **Login**: Use any email/password (demo mode)
2. **Chat**: Send messages in different channels
3. **Real-time**: Open multiple browser tabs to test real-time messaging

### API Endpoints

- `POST /api/auth/login` - Demo login
- `GET /api/auth/me` - Get current user
- `ws://localhost:8080/ws/chat?token=<token>` - WebSocket connection

### Database Access

- **PostgreSQL**: localhost:5432 (users: chatuser/chatpass)
- **MongoDB**: localhost:27017 (messages)
- **Redis**: localhost:6379 (sessions)

### Troubleshooting

1. **Port conflicts**: Change ports in docker-compose.yml
2. **WebSocket connection issues**: Check browser console for errors
3. **Database connection**: Ensure Docker services are running
4. **Backend startup**: Check application.yml configuration

### Code Structure

```
scalable-chat-platform/
├── backend/                 # Spring Boot API
│   ├── src/main/java/com/chatplatform/
│   │   ├── config/         # Configuration
│   │   ├── controller/     # REST controllers
│   │   ├── model/         # Data models
│   │   ├── repository/    # Data access
│   │   ├── service/       # Business logic
│   │   └── websocket/     # WebSocket handlers
│   └── src/main/resources/
│       └── application.yml # Application config
├── frontend/               # React app
│   └── src/
│       ├── components/    # UI components
│       ├── hooks/        # React hooks
│       ├── pages/        # Page components
│       └── types/        # TypeScript types
└── docker-compose.yml     # Infrastructure services
```

### Environment Variables

Backend:
- `SPRING_PROFILES_ACTIVE`: Application profile (default: local)
- `SERVER_PORT`: Backend port (default: 8080)
- `SPRING_DATASOURCE_URL`: PostgreSQL URL
- `SPRING_DATA_MONGODB_URI`: MongoDB URI
- `SPRING_REDIS_HOST`: Redis host
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka servers

Frontend:
- `REACT_APP_API_URL`: Backend API URL (default: http://localhost:8080)
- `REACT_APP_WS_URL`: WebSocket URL (default: ws://localhost:8080)
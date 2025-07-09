# Development Guide

## 🚀 Quick Setup

### Option 1: Automated Setup (Recommended)
```bash
./start-dev.sh
```

✅ **Automatic Kafka topic creation**
✅ **Cluster ID consistency checks**
✅ **Service health validation**
✅ **Volume persistence**

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

## 🛠️ Development Workflow

### Backend Development
- **Main application**: `backend/src/main/java/com/chatplatform/ChatPlatformApplication.java`
- **Run with**: `mvn spring-boot:run`
- **API endpoints**: http://localhost:8080
- **WebSocket endpoint**: ws://localhost:8080/ws/chat
- **Health check**: http://localhost:8080/api/health

**Java 17 Features Used**:
- Records for DTOs
- Pattern matching
- Functional interfaces
- Stream API enhancements
- Text blocks for SQL/JSON

### Frontend Development
- **Main component**: `frontend/src/App.tsx`
- **Local access**: `npm start` → http://localhost:3000
- **Network access**: `npm run start:network` → http://YOUR_IP:3000
- **Auto-reloads**: On file changes
- **Debug tools**: Network Info button in chat window

### 🧪 Testing the Application

1. **Login**: Use any email/password (demo mode)
2. **Chat**: Send messages in different channels
3. **Real-time**: Open multiple browser tabs/devices to test real-time messaging
4. **Network**: Test from different devices on same network
5. **Persistence**: Stop/start services to verify data persistence

**Multi-instance Testing**:
```bash
# Terminal 1
cd frontend && npm run start:network

# Terminal 2
cd frontend && PORT=3001 npm run start:network

# Terminal 3
cd frontend && PORT=3002 npm run start:network
```

Access at:
- http://localhost:3000
- http://localhost:3001
- http://localhost:3002

### API Endpoints

- `POST /api/auth/login` - Demo login
- `GET /api/auth/me` - Get current user
- `ws://localhost:8080/ws/chat?token=<token>` - WebSocket connection

### Database Access

- **PostgreSQL**: localhost:5432 (users: chatuser/chatpass)
- **MongoDB**: localhost:27017 (messages)
- **Redis**: localhost:6379 (sessions)

### 🔧 Troubleshooting

**🚨 Critical Issues**:
1. **Kafka cluster ID mismatch**: Run `./fix-kafka-only.sh`
2. **Topics missing**: Run `./start-dev.sh` (auto-creates topics)
3. **Data loss**: Use `./stop-dev.sh` not `docker-compose down -v`

**🔍 Debug Steps**:
1. **Check service health**: `docker-compose ps`
2. **View logs**: `docker-compose logs -f <service>`
3. **Network issues**: Click "Network Info" in chat window
4. **Backend logs**: Look for emoji indicators (✅ ❌ 📨 🚀)

**🎯 Common Solutions**:
- **Port conflicts**: `docker stop <conflicting-container>`
- **WebSocket issues**: Check browser console and backend logs
- **Database connection**: Verify all Docker services are running
- **IP detection**: Use `npm run start:network` for network access

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

### 🔧 Environment Variables

**Backend (application.yml)**:
- `SPRING_PROFILES_ACTIVE`: Application profile (default: local)
- `SERVER_PORT`: Backend port (default: 8080)
- `SPRING_DATASOURCE_URL`: PostgreSQL URL
- `SPRING_DATA_MONGODB_URI`: MongoDB URI
- `SPRING_REDIS_HOST`: Redis host
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka servers

**Frontend (Auto-detected)**:
- `REACT_APP_API_URL`: Backend API URL (auto-detected based on current IP)
- `REACT_APP_WS_URL`: WebSocket URL (auto-detected based on current IP)

**Manual Override (.env.local)**:
```bash
# Only needed for custom configurations
REACT_APP_API_URL=http://custom-ip:8080
REACT_APP_WS_URL=ws://custom-ip:8080
```

### 📊 Monitoring and Debugging

**Log Patterns to Watch**:
```bash
# Backend startup
🔧 Initializing Kafka topics configuration...
✅ Kafka topic validation successful

# Message flow
✅ Message sent to Kafka successfully
📨 Received message from Kafka
🚀 Published MessageDistributionEvent
📢 Received MessageDistributionEvent

# Network access
🌐 Dynamic CORS configured for IP ranges
```

**Frontend Debug Tools**:
- **Network Info Button**: Shows current IP detection and API configuration
- **Browser Console**: WebSocket connection status and errors
- **React Developer Tools**: Component state and props inspection
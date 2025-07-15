# 🐳 Docker Full Stack Setup

Complete containerized chat platform with all services running in Docker.

## 🚀 Quick Start

### **Startup Process** (4-6 minutes first time, 2-3 minutes after):
```bash
./start-full-stack.sh
```
**What happens:** Infrastructure → Backend → Frontend → Ready!  
**Access:** http://localhost:3000

### **Shutdown Process** (preserves your data):
```bash
# Safe shutdown - keeps all data
./stop-full-stack.sh

# Clean shutdown - removes all data  
./stop-full-stack.sh --clean

# Complete removal - removes images too
./stop-full-stack.sh --purge
```
**✅ Normal shutdown preserves:** Messages, users, conversations, search history

## 📋 What's Included

### Application Services
- **Frontend** (React + Nginx): `http://localhost:3000`
- **Backend** (Spring Boot): `http://localhost:8080`

### Infrastructure Services
- **PostgreSQL**: `localhost:5432` (user data)
- **MongoDB**: `localhost:27017` (messages)
- **Redis**: `localhost:6379` (caching)
- **Kafka**: `localhost:9092` (message queue)
- **Elasticsearch**: `localhost:9200` (search)

## 🔧 Script Options

### Startup Script (`start-full-stack.sh`)
```bash
# Normal start
./start-full-stack.sh

# Rebuild all images
./start-full-stack.sh --rebuild

# Start and follow logs
./start-full-stack.sh --logs
```

### Stop Script (`stop-full-stack.sh`)
```bash
# Safe stop - preserves all your data
./stop-full-stack.sh

# Clean stop - removes all data (fresh start next time)
./stop-full-stack.sh --clean

# Purge - removes everything (images, volumes, cached data)
./stop-full-stack.sh --purge
```

**🔒 Data Safety:** Normal shutdown keeps all messages, users, and conversations safe

## 🌐 Environment Configuration

### Frontend Configuration
The frontend automatically detects the environment and configures endpoints:

- **Development**: Uses localhost endpoints
- **Docker**: Uses container service names
- **Network IP**: Automatically detects local IP

### Backend Configuration
Environment variables control all connections:
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/chatdb
SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/chatdb

# Cache & Queue
SPRING_REDIS_HOST=redis
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# CORS & WebSocket
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://frontend:80
WEBSOCKET_ALLOWED_ORIGINS=http://localhost:3000,http://frontend:80
```

## 🔍 Monitoring & Debugging

### Check Service Health
```bash
# Overall status
docker-compose ps

# Backend health
curl http://localhost:8080/health

# Frontend health
curl http://localhost:3000/health
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Container Management
```bash
# Restart a service
docker-compose restart backend

# Rebuild a service
docker-compose build backend
docker-compose up -d backend

# Shell into container
docker-compose exec backend bash
docker-compose exec frontend sh
```

## 🔧 Custom Configuration

### Override Environment Variables
Create a `.env` file in the project root:
```bash
# Frontend URLs
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080

# Backend settings
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://my-domain.com
JWT_SECRET=my-custom-secret
```

### Custom Docker Compose
For production or custom setups, override configurations:
```bash
# Use custom compose file
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up
```

## 🌍 Network Access

### Access from Other Devices
The application automatically handles network access:

1. **Same Network**: Access via your computer's IP
   - Frontend: `http://YOUR_IP:3000`
   - Backend: `http://YOUR_IP:8080`

2. **Mobile Testing**: Use your local IP address
   - Find your IP: `ipconfig getifaddr en0` (macOS) or `hostname -I` (Linux)

3. **Custom Domain**: Update CORS settings in environment variables

## 🚨 Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Kill processes using ports
sudo lsof -ti:3000 | xargs kill -9
sudo lsof -ti:8080 | xargs kill -9
```

#### Backend Won't Start
```bash
# Check infrastructure services
docker-compose logs postgres
docker-compose logs mongodb
docker-compose logs kafka

# Restart in order
docker-compose up -d postgres mongodb redis
sleep 30
docker-compose up -d backend
```

#### Frontend Can't Connect
```bash
# Check environment variables
docker-compose exec frontend cat /usr/share/nginx/html/env-config.js

# Check nginx configuration
docker-compose exec frontend cat /etc/nginx/conf.d/default.conf
```

#### Performance Issues
```bash
# Check resource usage
docker stats

# Allocate more memory to Docker Desktop (recommended: 4GB+)
```

### Clean Reset
```bash
# Complete clean restart (removes all data and images)
./stop-full-stack.sh --purge
./start-full-stack.sh --rebuild
```

**⚠️ Warning:** This removes all your messages, users, and conversations

## 📊 Production Considerations

### Security
- Change default JWT secret
- Update database passwords
- Configure proper CORS origins
- Use HTTPS in production

### Performance
- Adjust JVM heap sizes
- Configure connection pools
- Set up load balancing
- Monitor resource usage

### Scaling
- Use Docker Swarm or Kubernetes
- Implement horizontal pod autoscaling
- Set up database clustering
- Configure Redis Cluster

## 🔮 Next Steps

1. **Test the full stack**: Follow the user acceptance testing checklist
2. **Performance tuning**: Optimize for your specific use case
3. **Production deployment**: Set up CI/CD pipeline
4. **Monitoring**: Add APM and logging solutions
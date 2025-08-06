# 🚀 Render Deployment Guide

Deploy the complete Scalable Chat Platform on Render's free tier with external managed services.

## 📋 Prerequisites

### Required Accounts
1. **[Render Account](https://render.com)** - Free tier with 750 hours/month
2. **[MongoDB Atlas](https://www.mongodb.com/atlas)** - Free M0 cluster (512MB)
3. **[Upstash Redis](https://upstash.com)** - Free tier (10k commands/day)
4. **GitHub Repository** - Code must be hosted on GitHub

### Local Requirements
- Git for cloning the repository
- Web browser for service configuration
- Java 17+ (for local development)
- Node.js 18+ (for local development)
- Maven 3.8+ (for local builds)

## 🏗️ Architecture Overview

**Single Service Deployment:**
- ✅ Spring Boot backend + React frontend in one container
- ✅ Render PostgreSQL (free 1GB database, expires after 30 days)
- ✅ External MongoDB Atlas (free 512MB, permanent)
- ✅ External Redis service (free tier, permanent)
- ✅ In-memory message queue (replaces Kafka for simplicity)

**Key Design Decisions:**
- **Single container**: Reduces complexity and resource usage
- **Static file serving**: React app served directly by Spring Boot
- **External managed services**: Offload database management
- **SSL everywhere**: All external connections use TLS

**Total Monthly Cost: $0.00** 🎉

## 📦 Step 1: External Services Setup

### 1.1 MongoDB Atlas Setup

1. **Create Account & Cluster**:
   - Go to [MongoDB Atlas](https://www.mongodb.com/atlas)
   - Sign up for free account
   - Create new M0 cluster (512MB storage, permanent)
   - Choose AWS us-east-1 region (recommended)

2. **Security Configuration**:
   - Create database user: `chatuser` with strong password
   - Whitelist IP addresses: `0.0.0.0/0` (for demo) or specific IPs
   - Note: For production, restrict to specific IP ranges

3. **Get Connection String**:
   ```
   mongodb+srv://chatuser:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/chatdb?retryWrites=true&w=majority
   ```

### 1.2 Redis Setup (Upstash - Recommended)

1. **Create Redis Database**:
   - Go to [Upstash](https://upstash.com)
   - Sign up for free account
   - Create new Redis database (10,000 commands/day)
   - Choose region closest to Render (US-East recommended)

2. **Get Connection URL**:
   - Copy the Redis URL from dashboard
   - Format: `rediss://default:PASSWORD@HOSTNAME:PORT`
   - ⚠️ **Important**: Use `rediss://` (with SSL) not `redis://`

3. **Alternative: Redis Cloud**:
   - [Redis Cloud](https://redis.io/cloud/) offers 30MB free
   - Same SSL requirement applies

## 🚀 Step 2: Render Services Setup

### 2.1 Create PostgreSQL Database

1. **Go to Render Dashboard** → **New** → **PostgreSQL**
2. **Configure Database**:
   - **Name**: `chat-platform-db`
   - **Database**: `chatdb` 
   - **User**: `chatuser`
   - **Region**: Oregon (US-West) or nearest to you
   - **Plan**: Free (1GB storage, expires after 30 days)
3. **Wait for Creation**: Takes 2-3 minutes
4. **Note Connection Details**: Render will auto-populate `DATABASE_URL`

### 2.2 Deploy Web Service

1. **Connect Repository**:
   - Go to Render Dashboard → **New** → **Web Service**
   - Connect your GitHub account
   - Select the `scalable-chat-platform` repository

2. **Configure Service**:
   - **Name**: `chat-platform` (or your preferred name)
   - **Region**: Oregon (same as database for lower latency)
   - **Branch**: `render-single-service-deployment`
   - **Build Command**: *Leave empty* (handled by Dockerfile)
   - **Start Command**: *Leave empty* (handled by Dockerfile)  
   - **Dockerfile Path**: `./Dockerfile.render`
   - **Health Check Path**: `/api/health/status`
   - **Plan**: Free

3. **Environment Variables**:
   Add these environment variables in the Render dashboard:

   ```bash
   # Database (Auto-populated by Render when you link PostgreSQL)
   DATABASE_URL=postgresql://user:pass@hostname:port/chatdb
   
   # External Services (Replace with your actual values)
   MONGODB_URI=mongodb+srv://chatuser:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/chatdb?retryWrites=true&w=majority
   REDIS_URL=rediss://default:YOUR_PASSWORD@hostname:port
   
   # Application Configuration
   SPRING_PROFILES_ACTIVE=render
   PORT=8080
   JWT_SECRET=your-very-secure-jwt-secret-key-change-this-in-production-min-32-chars
   
   # CORS Configuration (For demo - restrict in production)
   CORS_ALLOWED_ORIGINS=*
   WEBSOCKET_ALLOWED_ORIGINS=*
   
   # Logging Configuration
   LOGGING_LEVEL_COM_CHATPLATFORM=INFO
   LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=WARN
   LOGGING_LEVEL_ORG_HIBERNATE=WARN
   ```

4. **Link PostgreSQL Database**:
   - In the web service settings, link the PostgreSQL database you created
   - This automatically populates the `DATABASE_URL` environment variable

5. **Deploy**:
   - Click **"Create Web Service"**
   - First deployment takes 10-15 minutes (includes building React frontend)
   - Subsequent deployments are faster (~5 minutes)

## 🧪 Step 3: Testing Your Deployment

### 3.1 Health Check

Visit your app's health endpoint:
```
https://your-app-name.onrender.com/api/health/status
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z",
  "components": {
    "db": {
      "status": "UP",
      "details": {"database": "PostgreSQL", "validationQuery": "isValid()"}
    },
    "mongo": {
      "status": "UP", 
      "details": {"database": "MongoDB"}
    },
    "redis": {
      "status": "UP",
      "details": {"database": "Redis"}
    }
  }
}
```

### 3.2 Frontend Application

1. **Access Application**: Visit `https://your-app-name.onrender.com`
2. **Test Registration**: Create a new account with any email/password
3. **Test Login**: Login with the created credentials
4. **Test Messaging**: 
   - Open the app in multiple browser tabs
   - Login with different accounts in each tab
   - Send messages and verify real-time delivery
   - Check read receipts (checkmarks) appear correctly

### 3.3 API Endpoints Testing

Test these key endpoints:
- **Health**: `GET /api/health/status`
- **Authentication**: `POST /api/auth/login`
- **Conversations**: `GET /api/conversations` (requires auth)
- **WebSocket**: `wss://your-app-name.onrender.com/ws/chat`

## ⚠️ Free Tier Limitations & Considerations

### Render Free Tier Limits
- ✅ **750 build hours/month** (sufficient for 24/7 if single service)
- ⚠️ **Auto-sleep after 15 minutes** of inactivity (cold starts take ~1 minute)
- ⚠️ **PostgreSQL expires after 30 days** (data is lost - can recreate)
- ⚠️ **Custom domains not available** (*.onrender.com subdomain only)
- ⚠️ **Limited concurrent connections** (~100)

### External Services (Permanent)
- ✅ **MongoDB Atlas M0**: 512MB storage, permanent, good performance
- ✅ **Upstash Redis**: 10k commands/day, permanent, sufficient for demos
- ✅ **Both services have generous free tiers** suitable for development/demos

### Cold Start Mitigation
- App sleeps after 15 minutes of inactivity
- First request after sleep takes ~60 seconds to respond
- Consider using uptime monitoring services (UptimeRobot, etc.) to keep app warm
- Production plans eliminate auto-sleep

## 🎯 Production Migration Path

When ready for production, upgrade to:

### Render Pro Features ($7-25/month)
- ✅ **No auto-sleep** - always available
- ✅ **Custom domains** with SSL certificates
- ✅ **Higher resource limits** and better performance
- ✅ **Priority support** and SLA guarantees

### Security Hardening
```bash
# Replace wildcard CORS with specific domains
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
WEBSOCKET_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Use strong JWT secret (32+ characters)
JWT_SECRET=super-secure-random-string-with-numbers-and-symbols-123!@#

# Enable production logging
SPRING_PROFILES_ACTIVE=render,prod
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_CHATPLATFORM=INFO
```

### Monitoring & Observability
- **Application Monitoring**: Integrate with Sentry, DataDog, or New Relic
- **Uptime Monitoring**: UptimeRobot, Pingdom, or StatusCake
- **Log Aggregation**: Consider external log services for persistence
- **Database Monitoring**: MongoDB Atlas and Render provide built-in monitoring

## 🔧 Troubleshooting

### Common Issues

**1. Build Fails with "Package not found"**
```bash
# Solution: Clear build cache in Render dashboard
# Settings → Build & Deploy → Clear build cache → Deploy
```

**2. Database Connection Issues**
```bash
# Check DATABASE_URL format:
postgresql://username:password@hostname:port/database

# Verify PostgreSQL service is linked in Render dashboard
```

**3. MongoDB Connection Fails**
```bash
# Common issues:
- Wrong IP whitelist (use 0.0.0.0/0 for testing)
- Incorrect connection string format
- Missing database name in URI
- Wrong username/password

# Test connection string format:  
mongodb+srv://user:pass@cluster.mongodb.net/dbname?retryWrites=true&w=majority
```

**4. Redis Connection Issues**
```bash
# Ensure using rediss:// (SSL) not redis://
# Check Upstash dashboard for correct URL
# Format: rediss://default:password@hostname:port
```

**5. WebSocket Connection Fails**
```bash
# Check browser console for WebSocket errors
# Verify WEBSOCKET_ALLOWED_ORIGINS includes your domain
# Test WebSocket URL: wss://your-app.onrender.com/ws/chat
```

**6. App Sleeps Too Frequently**
```bash
# Free tier limitation - app sleeps after 15 min inactivity
# Solutions:
# 1. Use external uptime monitoring (free services available)
# 2. Upgrade to paid plan to eliminate sleep
# 3. Accept cold starts for demo purposes
```

### Debug Steps

1. **Check Render Logs**:
   - Go to your service dashboard
   - Click "Logs" tab
   - Look for startup errors or runtime issues

2. **Verify Environment Variables**:
   - Settings → Environment → verify all variables are set
   - Check for typos in URLs and secrets

3. **Test External Services**:
   - Test MongoDB connection from MongoDB Compass
   - Test Redis connection from Upstash console
   - Verify network access from Render IPs

4. **Frontend Issues**:
   - Check browser developer console
   - Verify API endpoints return correct responses
   - Test WebSocket connection manually

### Important Log Patterns

Key log messages to monitor:
- `🚀 Started in-memory message processor` - Message system initialized
- `✅ Message distributed` - Event processing working
- `🔌 WebSocket connected` - Real-time features active
- `💾 Message saved` - Database writes working
- `RedisConnectionFailureException` - Redis connectivity issues

### Performance Optimization

**For Better Performance on Free Tier**:
1. **Keep App Warm**: Use external monitoring to ping every 10 minutes
2. **Optimize Build**: Enable build cache for faster deployments  
3. **Database Indexing**: Ensure MongoDB has proper indexes for chat queries
4. **Redis Optimization**: Use Redis for session storage and caching
5. **Frontend Optimization**: Built-in React optimizations are already enabled

## 📞 Support Resources

- **Render Documentation**: [render.com/docs](https://render.com/docs)
- **MongoDB Atlas Docs**: [docs.atlas.mongodb.com](https://docs.atlas.mongodb.com)
- **Upstash Documentation**: [docs.upstash.com](https://docs.upstash.com)
- **Project Issues**: Use GitHub Issues for bugs and feature requests

## 🔧 Configuration Files Reference

### Application Configuration (`application-render.yml`)
Key configurations for Render deployment:

```yaml
spring:
  # PostgreSQL Configuration
  datasource:
    url: ${DATABASE_URL}
    hikari:
      maximum-pool-size: 3      # Conservative for free tier
      
  # MongoDB Configuration
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: chatdb
      
  # Redis Configuration with SSL
  redis:
    url: ${REDIS_URL}
    ssl: true                   # Critical for Upstash
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 3
        
# Server optimizations for free tier
server:
  port: ${PORT:8080}
  tomcat:
    threads:
      max: 50                   # Conservative threading
```

### Docker Configuration (`Dockerfile.render`)
Multi-stage build optimized for Render:

```dockerfile
# Stage 1: Build frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --only=production
COPY frontend/ .
RUN npm run build

# Stage 2: Build backend
FROM maven:3.8.6-openjdk-17-slim AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /app/frontend/build ./src/main/resources/static
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

## 💰 Cost Analysis & Upgrade Path

### Free Tier Limits
- **Render**: 750 hours/month (sufficient for single always-on service)
- **MongoDB Atlas**: 512MB storage (permanent)
- **Upstash Redis**: 10,000 commands/day (permanent)
- **Total Cost**: $0/month for demo/development

### Production Upgrade Path
When ready for production, upgrade to:

**Render Pro Features ($7-25/month)**
- ✅ **No auto-sleep** - always available
- ✅ **Custom domains** with SSL certificates
- ✅ **Higher resource limits** and better performance
- ✅ **Priority support** and SLA guarantees

**Security Hardening for Production**
```bash
# Replace wildcard CORS with specific domains
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
WEBSOCKET_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Use strong JWT secret (32+ characters)
JWT_SECRET=super-secure-random-string-with-numbers-and-symbols-123!@#

# Enable production logging
SPRING_PROFILES_ACTIVE=render,prod
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_CHATPLATFORM=INFO
```

**Total Production Cost**: ~$15-25/month

## ✅ Deployment Checklist

- [ ] MongoDB Atlas cluster created and configured
- [ ] Redis database created (Upstash recommended)
- [ ] Render PostgreSQL database created  
- [ ] GitHub repository connected to Render
- [ ] All environment variables configured
- [ ] First deployment completed successfully
- [ ] Health check endpoint returns "UP" status
- [ ] Frontend loads correctly
- [ ] Real-time messaging works between browser tabs
- [ ] Read receipts display correctly
- [ ] Search functionality works
- [ ] Authentication flow complete

🎉 **Congratulations!** Your Scalable Chat Platform is now live on Render with modern messaging features including real-time chat, read receipts, and advanced search capabilities.
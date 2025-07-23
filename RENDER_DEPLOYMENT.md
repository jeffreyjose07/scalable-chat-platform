# 🚀 Render Deployment Guide - Single Service Chat Platform

This guide covers deploying the complete chat platform as a single service on Render's free tier.

## 📋 Prerequisites

1. **Render Account**: Sign up at [render.com](https://render.com)
2. **MongoDB Atlas Account**: Free M0 cluster at [mongodb.com](https://www.mongodb.com/atlas)
3. **Redis Service**: Free tier from [Upstash](https://upstash.com) or [Redis Cloud](https://redis.io/cloud/)
4. **GitHub Repository**: Your code must be on GitHub

## 🏗️ Architecture Overview

**Single Service Deployment:**
- ✅ Spring Boot backend + React frontend in one container
- ✅ Render PostgreSQL (free 1GB database)
- ✅ External MongoDB Atlas (free 512MB)
- ✅ External Redis service (free tier)
- ✅ In-memory message queue (replaces Kafka)

**Total Cost: $0.00/month** 🎉

## 📦 Step 1: External Services Setup

### 1.1 MongoDB Atlas Setup
1. Go to [MongoDB Atlas](https://www.mongodb.com/atlas)
2. Create free M0 cluster (512MB storage)
3. Create database user and get connection string
4. Whitelist all IP addresses (`0.0.0.0/0`) for demo access
5. Save connection string: `mongodb+srv://username:password@cluster.mongodb.net/chatdb?retryWrites=true&w=majority`

### 1.2 Redis Setup (Choose one)
**Option A: Upstash Redis (Recommended)**
1. Go to [Upstash](https://upstash.com)
2. Create free Redis database (10,000 commands/day)
3. Get Redis URL: `rediss://default:password@hostname:port` ⚠️ **Note: Use `rediss://` (with SSL)**

**Option B: Redis Cloud**
1. Go to [Redis Cloud](https://redis.io/cloud/)
2. Create 30MB free database
3. Get Redis URL: `rediss://default:password@hostname:port` ⚠️ **Note: Use `rediss://` (with SSL)**

## 🚀 Step 2: Render Deployment

### 2.1 Create PostgreSQL Database
1. Go to Render Dashboard → New → PostgreSQL
2. Configure:
   - **Name**: `chat-platform-db`
   - **Database**: `chatdb`
   - **User**: `chatuser`
   - **Region**: Oregon (or nearest)
   - **Plan**: Free
3. Wait for database creation (2-3 minutes)
4. Note the connection details

### 2.2 Deploy Web Service
1. Go to Render Dashboard → New → Web Service
2. Connect your GitHub repository
3. Configure:
   - **Name**: `chat-platform`
   - **Region**: Oregon (same as database)
   - **Branch**: `render-single-service-deployment`
   - **Build Command**: *Leave empty* (handled by Dockerfile)
   - **Start Command**: *Leave empty* (handled by Dockerfile)
   - **Dockerfile Path**: `./Dockerfile.render`
   - **Plan**: Free

### 2.3 Environment Variables Setup
Add these environment variables to your Render web service:

```bash
# Database (Auto-set by Render)
DATABASE_URL=postgresql://user:pass@hostname:port/chatdb

# External Services
MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/chatdb?retryWrites=true&w=majority
REDIS_URL=rediss://default:password@hostname:port

# Application Config
SPRING_PROFILES_ACTIVE=render
PORT=8080
JWT_SECRET=your-very-secure-jwt-secret-key-change-this-in-production

# CORS (For demo - use specific domains in production)
CORS_ALLOWED_ORIGINS=*
WEBSOCKET_ALLOWED_ORIGINS=*

# Logging
LOGGING_LEVEL_COM_CHATPLATFORM=INFO
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=WARN
LOGGING_LEVEL_ORG_HIBERNATE=WARN
```

### 2.4 Deploy
1. Click **"Create Web Service"**
2. Wait for build and deployment (10-15 minutes first time)
3. Your app will be available at: `https://your-app-name.onrender.com`

## 🧪 Step 3: Testing Deployment

### 3.1 Health Check
Visit: `https://your-app-name.onrender.com/api/health/status`

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "mongo": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

### 3.2 Frontend Access
1. Visit: `https://your-app-name.onrender.com`
2. Should load the React frontend
3. Register/login with demo credentials
4. Test real-time messaging between browser tabs

### 3.3 API Testing
Test key endpoints:
- `GET /api/health/status` - Health check
- `POST /api/auth/login` - Authentication
- `GET /api/conversations` - User conversations
- WebSocket at: `wss://your-app-name.onrender.com/ws/chat`

## ⚠️ Free Tier Limitations

### Render Free Tier
- ✅ **750 hours/month** (enough for 24/7 if only one service)
- ⚠️ **Auto-sleep after 15 minutes** of inactivity
- ⚠️ **Cold start delay** (~1 minute to wake up)
- ⚠️ **PostgreSQL expires after 30 days** (can be recreated)

### External Services
- ✅ **MongoDB Atlas**: 512MB storage (permanent)
- ✅ **Redis**: 10k-30MB depending on provider (permanent)

## 🎯 Production Recommendations

For production deployment:
1. **Upgrade Render plan** to avoid sleep and get custom domains
2. **Use specific CORS origins** instead of `*`
3. **Enable SSL** for WebSocket connections
4. **Set up monitoring** and error tracking
5. **Configure backup strategy**
6. **Use environment-specific JWT secrets**

## 🐛 Troubleshooting

### Build Failures
- Check Dockerfile syntax
- Ensure frontend dependencies are available
- Verify Maven build succeeds locally

### Runtime Issues
- Check Render logs: Service → Logs tab
- Verify environment variables are set correctly
- Test external service connections

### Database Connection Issues
- Verify DATABASE_URL format
- Check MongoDB Atlas whitelist settings
- Test Redis URL format and credentials
- **Redis SSL Issues**: Ensure using `rediss://` not `redis://` for Upstash/external Redis

### Frontend Not Loading
- Check if static resources are built correctly
- Verify WebController routing
- Check CORS configuration

## 📊 Monitoring

### Render Dashboard
- **Metrics**: CPU, Memory usage
- **Logs**: Application and system logs
- **Events**: Deploy history

### Application Logs
Key log messages to monitor:
- `🚀 Started in-memory message processor` - Message system OK
- `💾 Message saved` - Database writes working
- `✅ Message distributed` - Event system working
- `🔌 WebSocket connected` - Real-time features working

## 🎉 Success!

Your chat platform is now live at `https://your-app-name.onrender.com`!

**Demo Features:**
- ✅ Real-time messaging
- ✅ User authentication
- ✅ Conversation management
- ✅ Message persistence
- ✅ Responsive design
- ✅ Professional portfolio showcase

Perfect for demonstrating your full-stack development skills! 🚀
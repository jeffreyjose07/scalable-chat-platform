# Production Deployment Guide

## Overview
This guide helps you deploy the complete Scalable Chat Platform to production using:
- **Railway.com** for Spring Boot backend + databases
- **Vercel** for React frontend
- **External services** for Kafka & Elasticsearch

## Prerequisites
- Railway account (free tier available)
- Vercel account (free tier available)
- GitHub repository access
- Domain name (optional)

## Backend Deployment (Railway)

### 1. Database Setup on Railway

**PostgreSQL Database:**
```bash
# Create PostgreSQL service
railway add --template postgresql
# Note the DATABASE_URL from Railway dashboard
```

**Redis Cache:**
```bash
# Create Redis service
railway add --template redis
# Note the REDIS_URL from Railway dashboard
```

**MongoDB Service:**
```bash
# Create MongoDB service
railway add --template mongodb
# Note the MONGODB_URL from Railway dashboard
```

### 2. External Services Setup

**Kafka Service (Upstash Kafka):**
1. Go to [Upstash.com](https://upstash.com)
2. Create Kafka cluster
3. Get connection details:
   - `KAFKA_BOOTSTRAP_SERVERS`
   - `KAFKA_SASL_JAAS_CONFIG`
   - `KAFKA_SASL_MECHANISM=SCRAM-SHA-512`
   - `KAFKA_SECURITY_PROTOCOL=SASL_SSL`

**Elasticsearch Service (Elastic Cloud):**
1. Go to [Elastic Cloud](https://cloud.elastic.co)
2. Create deployment
3. Get connection details:
   - `ELASTICSEARCH_URL`
   - `ELASTICSEARCH_USERNAME`
   - `ELASTICSEARCH_PASSWORD`

### 3. Railway Backend Deployment

**Step 1: Connect Repository**
```bash
# Install Railway CLI
npm install -g @railway/cli

# Login to Railway
railway login

# Initialize project
railway init

# Link to your backend
railway link
```

**Step 2: Set Environment Variables**
```bash
# Database connections (auto-configured by Railway)
railway variables set DATABASE_URL=$DATABASE_URL
railway variables set REDIS_URL=$REDIS_URL
railway variables set MONGODB_URL=$MONGODB_URL

# External services
railway variables set KAFKA_BOOTSTRAP_SERVERS="your-upstash-endpoint"
railway variables set KAFKA_SASL_JAAS_CONFIG="your-sasl-config"
railway variables set KAFKA_SASL_MECHANISM="SCRAM-SHA-512"
railway variables set KAFKA_SECURITY_PROTOCOL="SASL_SSL"

railway variables set ELASTICSEARCH_URL="your-elastic-endpoint"
railway variables set ELASTICSEARCH_USERNAME="your-username"
railway variables set ELASTICSEARCH_PASSWORD="your-password"

# Application settings
railway variables set SPRING_PROFILES_ACTIVE="production"
railway variables set JWT_SECRET="your-secure-jwt-secret"
railway variables set CORS_ALLOWED_ORIGINS="https://your-frontend.vercel.app"
railway variables set WEBSOCKET_ALLOWED_ORIGINS="https://your-frontend.vercel.app"
```

**Step 3: Deploy Backend**
```bash
# Deploy to Railway
railway up

# Check deployment status
railway status

# View logs
railway logs
```

## Frontend Deployment (Vercel)

### 1. Vercel Setup

**Install Vercel CLI:**
```bash
npm install -g vercel
```

**Login to Vercel:**
```bash
vercel login
```

### 2. Configure Environment Variables

**In Vercel Dashboard:**
1. Go to your project settings
2. Add environment variables:
   - `REACT_APP_API_BASE_URL`: Your Railway backend URL
   - `REACT_APP_WS_URL`: Your Railway WebSocket URL (replace http with ws)

### 3. Deploy Frontend

**Option 1: Auto-deploy (Recommended)**
1. Connect GitHub repository to Vercel
2. Set build settings:
   - Build command: `cd frontend && npm run build`
   - Output directory: `frontend/build`
3. Deploy automatically on push

**Option 2: Manual deploy**
```bash
# Build frontend
cd frontend
npm run build

# Deploy to Vercel
vercel --prod
```

## Production Configuration

### Backend Environment Variables (Railway)
```env
# Core Configuration
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
JWT_SECRET=your-very-secure-jwt-secret-change-this

# Database URLs (auto-configured by Railway)
DATABASE_URL=postgresql://user:pass@host:port/db
REDIS_URL=redis://host:port
MONGODB_URL=mongodb://host:port/db

# External Services
KAFKA_BOOTSTRAP_SERVERS=your-kafka-endpoint
KAFKA_SASL_JAAS_CONFIG=your-sasl-config
KAFKA_SASL_MECHANISM=SCRAM-SHA-512
KAFKA_SECURITY_PROTOCOL=SASL_SSL

ELASTICSEARCH_URL=your-elasticsearch-endpoint
ELASTICSEARCH_USERNAME=your-username
ELASTICSEARCH_PASSWORD=your-password

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
WEBSOCKET_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

### Frontend Environment Variables (Vercel)
```env
REACT_APP_API_BASE_URL=https://your-backend.railway.app
REACT_APP_WS_URL=wss://your-backend.railway.app
```

## Cost Estimation

### Railway (Backend + Databases)
- **Hobby Plan**: $5/month minimum
- **Backend App**: ~$10-15/month (1GB RAM, 0.5 vCPU)
- **PostgreSQL**: ~$5-10/month (512MB RAM)
- **Redis**: ~$3-5/month (256MB RAM)
- **MongoDB**: ~$5-10/month (512MB RAM)
- **Total**: ~$28-45/month

### Vercel (Frontend)
- **Hobby Plan**: Free (100GB bandwidth)
- **Pro Plan**: $20/month (1TB bandwidth)

### External Services
- **Upstash Kafka**: $10-20/month
- **Elastic Cloud**: $16-50/month

### **Total Monthly Cost**: ~$54-115/month

## Free Tier Limitations

### Railway Free Credits
- $5 in credits for 30 days
- Good for testing deployment

### Vercel Free Tier
- 100GB bandwidth/month
- Custom domain support
- Automatic HTTPS

## Testing Your Deployment

### Health Checks
```bash
# Backend health
curl https://your-backend.railway.app/api/health/status

# Frontend access
curl https://your-frontend.vercel.app
```

### Monitoring
- Railway provides built-in logs and metrics
- Vercel provides analytics and performance insights
- Set up alerts for service outages

## Troubleshooting

### Common Issues

**Backend Won't Start:**
1. Check Railway logs: `railway logs`
2. Verify environment variables
3. Check database connections

**Frontend Can't Connect:**
1. Verify CORS settings
2. Check API endpoint URLs
3. Verify WebSocket URLs

**Database Connection Issues:**
1. Check DATABASE_URL format
2. Verify network connectivity
3. Check database service status

### Performance Optimization

**Backend:**
- Use production JVM flags
- Enable HTTP/2 and compression
- Configure connection pooling

**Frontend:**
- Enable Vercel Edge Network
- Use static asset optimization
- Configure proper caching headers

## Security Considerations

### Environment Variables
- Never commit secrets to Git
- Use Railway/Vercel secret management
- Rotate JWT secrets regularly

### Database Security
- Use strong passwords
- Enable SSL connections
- Configure proper firewall rules

### CORS Configuration
- Specify exact domains
- Don't use wildcard in production
- Configure proper headers

## Scaling Considerations

### Horizontal Scaling
- Railway supports automatic scaling
- Vercel provides global CDN
- External services handle their own scaling

### Database Scaling
- PostgreSQL: Consider read replicas
- MongoDB: Use sharding for large datasets
- Redis: Consider clustering for high availability

This deployment preserves your complete architecture while leveraging managed services for reliability and scalability.
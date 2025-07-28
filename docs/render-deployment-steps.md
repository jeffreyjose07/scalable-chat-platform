# 🚀 Render Deployment Steps - JWT Secured Chat Platform

Follow these exact steps to deploy your JWT-secured chat platform to Render.

## 📋 Pre-Deployment Checklist

- [ ] Generate JWT secret using the provided script
- [ ] Set up MongoDB Atlas account (free tier)
- [ ] Set up Upstash Redis account (free tier)
- [ ] Have your GitHub repository ready
- [ ] Ensure you're on the `render-single-service-deployment` branch

## 🔧 Step 1: Generate JWT Secret

Run this command locally to generate a secure JWT secret:

```bash
cd /Users/jeffrey.jose/cursorProjects/scalable-chat-platform
./scripts/generate-jwt-secret.sh
```

**Copy the generated `JWT_SECRET` value** - you'll need it in Step 4.

## 🗄️ Step 2: Set Up External Services

### MongoDB Atlas (Free Tier)

1. Go to [MongoDB Atlas](https://www.mongodb.com/atlas)
2. Create a free account and cluster
3. Create a database user with read/write permissions
4. Whitelist `0.0.0.0/0` for network access (or Render IP ranges)
5. Copy the connection string:
   ```
   mongodb+srv://username:password@cluster.mongodb.net/chatdb?retryWrites=true&w=majority
   ```

### Upstash Redis (Free Tier)

1. Go to [Upstash](https://upstash.com/)
2. Create a free account and Redis database
3. Copy the Redis URL:
   ```
   redis://default:password@host:port
   ```

## 🌐 Step 3: Deploy to Render

### Option A: Using Render Dashboard

1. Go to [Render Dashboard](https://dashboard.render.com/)
2. Click "New +" → "Blueprint"
3. Connect your GitHub repository
4. Select the branch: `render-single-service-deployment`
5. Render will detect the `render.yaml` file automatically
6. Click "Apply"

### Option B: Using Render CLI (Alternative)

```bash
# Install Render CLI
npm install -g @render-com/cli

# Login to Render
render login

# Deploy using blueprint
render blueprint launch
```

## 🔐 Step 4: Configure Environment Variables

After deployment starts, configure these **critical security variables** in the Render dashboard:

### Navigate to Your Service

1. Go to Render Dashboard → Your Service → Environment tab

### Set Required Variables

**🔑 JWT Security (CRITICAL)**
```
JWT_SECRET = <paste-your-generated-secret-from-step-1>
```

**👤 Admin Credentials (CRITICAL)**
```
ADMIN_PASSWORD = <create-strong-password>
```
*Use a strong password with uppercase, lowercase, numbers, and symbols*

**🗄️ External Services**
```
MONGODB_URI = <your-mongodb-atlas-connection-string>
REDIS_URL = <your-upstash-redis-url>
```

**🌍 Domain Configuration**
```
CORS_ALLOWED_ORIGINS = https://your-actual-render-app-url.onrender.com
WEBSOCKET_ALLOWED_ORIGINS = https://your-actual-render-app-url.onrender.com
```
*Replace with your actual Render app URL after deployment*

### Auto-Set Variables (No Action Needed)

These are automatically configured by Render:
- `DATABASE_URL` (PostgreSQL connection)
- `PORT` (8080)
- Other application settings from render.yaml

## 🔄 Step 5: Update Domain Configuration

After your app is deployed, you'll get a Render URL like:
`https://chat-platform-xyz123.onrender.com`

Update these environment variables with your actual URL:

```
CORS_ALLOWED_ORIGINS = https://chat-platform-xyz123.onrender.com
WEBSOCKET_ALLOWED_ORIGINS = https://chat-platform-xyz123.onrender.com
```

## ✅ Step 6: Verify Deployment

### Check Health Endpoint

```bash
curl https://your-app-url.onrender.com/api/health/status
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Test Authentication

1. **Register a new user**:
```bash
curl -X POST https://your-app-url.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com", 
    "password": "TestPass123!",
    "displayName": "Test User"
  }'
```

2. **Login**:
```bash
curl -X POST https://your-app-url.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }'
```

3. **Test JWT token** (use token from login response):
```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
     https://your-app-url.onrender.com/api/auth/me
```

4. **Test logout** (token should be blacklisted):
```bash
curl -X POST https://your-app-url.onrender.com/api/auth/logout \
     -H "Authorization: Bearer <your-jwt-token>"
```

## 🛡️ Step 7: Security Verification

### JWT Security Checklist

- [ ] JWT tokens are generated with issuer/audience claims
- [ ] Token expiry is 4 hours (14400000 ms)
- [ ] Logout blacklists tokens in Redis
- [ ] Invalid tokens are rejected
- [ ] CORS only allows your domain

### Test Security Features

1. **Token Blacklisting**:
   - Login to get a token
   - Logout with that token  
   - Try to use the same token → Should be rejected

2. **Claims Validation**:
   - Try using a token from a different environment → Should fail

3. **CORS Protection**:
   - Try API calls from unauthorized domains → Should fail

## 📊 Step 8: Monitor Your Application

### Render Dashboard Monitoring

1. **Logs**: Monitor application logs for errors
2. **Metrics**: Check CPU, memory, and response times
3. **Health Checks**: Ensure health endpoint returns 200 OK

### Key Metrics to Watch

- **Authentication Success Rate**: Monitor login/registration success
- **Token Blacklist Size**: Redis memory usage for blacklisted tokens
- **Response Times**: API and WebSocket performance
- **Error Rates**: Application and security errors

## 🚨 Step 9: Troubleshooting

### Common Issues and Solutions

**❌ JWT_SECRET not set**
```
Error: JWT secret must be configured
Solution: Set JWT_SECRET in Render environment variables
```

**❌ MongoDB connection failed**
```
Error: MongoDB connection timeout
Solution: Check MONGODB_URI and whitelist 0.0.0.0/0 in Atlas
```

**❌ Redis connection failed**
```
Error: Unable to connect to Redis
Solution: Verify REDIS_URL format and Upstash configuration
```

**❌ CORS errors**
```
Error: CORS policy blocked
Solution: Update CORS_ALLOWED_ORIGINS with correct domain
```

**❌ Health check failing**
```
Error: Service unhealthy
Solution: Check application logs and database connections
```

## 📝 Step 10: Final Security Checklist

Before going live, verify:

- [ ] Strong JWT secret (256+ bits) is set
- [ ] Admin password is strong and secure
- [ ] CORS is restricted to your domain only
- [ ] All external services (MongoDB, Redis) are connected
- [ ] Health checks are passing
- [ ] Authentication flow works end-to-end
- [ ] Token blacklisting functions properly
- [ ] Logs show no security warnings
- [ ] All sensitive data is in environment variables (not code)

## 🎉 Deployment Complete!

Your JWT-secured chat platform is now deployed on Render with:

- ✅ Enterprise-grade JWT security
- ✅ Token blacklisting with Redis
- ✅ Secure frontend token storage
- ✅ Comprehensive error handling
- ✅ Production-ready configuration
- ✅ Monitoring and health checks

**Your app is live at**: `https://your-app-name.onrender.com`

## 📞 Support

If you encounter issues:

1. Check Render service logs in the dashboard
2. Verify all environment variables are set correctly
3. Test external service connections (MongoDB, Redis)
4. Review the security configuration guide: `/SECURITY.md`

---

**🔒 Security Notice**: Your JWT implementation now exceeds industry security standards. Regular monitoring and updates are recommended to maintain security posture.
# 🚀 Render Deployment - JWT Security Configuration

This guide covers deploying the JWT-secured chat platform to Render with all security features enabled.

## 📋 Pre-Deployment Checklist

- [ ] JWT secret generation script ready
- [ ] Environment variables configured
- [ ] Security features tested locally
- [ ] Redis add-on configured for token blacklisting

## 🔑 Step 1: Generate Production JWT Secret

Run the JWT secret generation script locally:

```bash
cd /Users/jeffrey.jose/cursorProjects/scalable-chat-platform
./scripts/generate-jwt-secret.sh
```

Copy the generated `JWT_SECRET` value - you'll need it for Render environment variables.

## 🌐 Step 2: Render Service Configuration

### Backend Service (Spring Boot)

1. **Create/Update `render.yaml`** (if using Blueprint):

```yaml
services:
  - type: web
    name: chat-platform-backend
    env: java
    buildCommand: |
      cd backend
      ./mvnw clean package -DskipTests
    startCommand: |
      cd backend
      java -jar target/chat-platform-backend-*.jar
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: JWT_SECRET
        sync: false  # Set manually in Render dashboard
      - key: JWT_EXPIRATION
        value: "14400000"  # 4 hours
      - key: JWT_ISSUER
        value: "chat-platform-backend"
      - key: JWT_AUDIENCE
        value: "chat-platform-users"
      - key: SPRING_DATASOURCE_URL
        fromDatabase:
          name: chat-platform-db
          property: connectionString
      - key: SPRING_DATA_MONGODB_URI
        fromDatabase:
          name: chat-platform-mongodb
          property: connectionString
      - key: SPRING_REDIS_URL
        fromDatabase:
          name: chat-platform-redis
          property: connectionString
      - key: CORS_ALLOWED_ORIGINS
        value: "https://your-frontend-app.onrender.com"

databases:
  - name: chat-platform-db
    databaseName: chatdb
    user: chatuser
  - name: chat-platform-mongodb
    databaseName: chatdb
  - name: chat-platform-redis
```

### Frontend Service (React)

2. **Frontend Environment Variables**:

```yaml
  - type: web
    name: chat-platform-frontend
    env: static
    buildCommand: |
      cd frontend
      npm install
      npm run build
    staticPublishPath: frontend/build
    envVars:
      - key: REACT_APP_API_URL
        value: "https://your-backend-service.onrender.com"
      - key: NODE_ENV
        value: "production"
```

## 🔧 Step 3: Manual Environment Variable Setup

### In Render Dashboard:

1. **Go to your backend service** → Environment tab

2. **Add these environment variables**:

```
JWT_SECRET = <paste-your-generated-secret-here>
JWT_EXPIRATION = 14400000
JWT_ISSUER = chat-platform-backend
JWT_AUDIENCE = chat-platform-users
SPRING_PROFILES_ACTIVE = prod
CORS_ALLOWED_ORIGINS = https://scalable-chat-platform.onrender.com/
```

3. **Database URLs** (Render auto-generates these):
```
SPRING_DATASOURCE_URL = <from-render-postgres-addon>
SPRING_DATA_MONGODB_URI = <from-render-mongodb-addon>
SPRING_REDIS_URL = <from-render-redis-addon>
```

## 📦 Step 4: Add Redis Add-on

1. In Render dashboard, go to your service
2. Click "Add-ons" tab
3. Add "Redis" add-on
4. This automatically sets `REDIS_URL` environment variable
5. Update your Spring configuration to use this URL

## 🔄 Step 5: Update Spring Configuration for Render

Create `backend/src/main/resources/application-prod.yml`:

```yaml
spring:
  profiles:
    active: prod
  
  datasource:
    url: ${DATABASE_URL:${SPRING_DATASOURCE_URL}}
    username: ${SPRING_DATASOURCE_USERNAME:}
    password: ${SPRING_DATASOURCE_PASSWORD:}
    driver-class-name: org.postgresql.Driver
  
  data:
    mongodb:
      uri: ${MONGODB_URI:${SPRING_DATA_MONGODB_URI}}
    
    redis:
      url: ${REDIS_URL:${SPRING_REDIS_URL}}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

server:
  port: ${PORT:8080}

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION:14400000}
    issuer: ${JWT_ISSUER:chat-platform-backend}
    audience: ${JWT_AUDIENCE:chat-platform-users}
  
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:*}

logging:
  level:
    com.chatplatform: INFO
    org.springframework.web.socket: WARN
    org.springframework.security: INFO
```

## 🛡️ Step 6: Security Verification

### Test Security Features After Deployment:

1. **JWT Secret Validation**:
```bash
# Test with curl
curl -H "Authorization: Bearer invalid-token" \
     https://your-backend.onrender.com/api/auth/me
```

2. **Token Blacklisting**:
```bash
# Login, then logout, then try to use the same token
curl -X POST https://your-backend.onrender.com/api/auth/logout \
     -H "Authorization: Bearer <your-token>"
```

3. **Claims Validation**:
   - Try tokens from different environments
   - Verify issuer/audience validation works

## 📊 Step 7: Production Monitoring

### Add Health Checks in `render.yaml`:

```yaml
services:
  - type: web
    name: chat-platform-backend
    healthCheckPath: /actuator/health
    # ... other config
```

### Monitor Security Metrics:

1. **Render Logs** - Monitor authentication failures
2. **Redis Memory** - Track blacklisted token count
3. **Response Times** - Ensure security doesn't impact performance

## 🚨 Step 8: Security Incident Response

### Emergency Token Revocation:

1. **Access Render Redis**:
```bash
# Connect to Redis via Render dashboard shell
redis-cli -u $REDIS_URL
```

2. **Blacklist All Tokens** (Nuclear Option):
```bash
# Change JWT secret to invalidate all tokens
# In Render dashboard, update JWT_SECRET environment variable
```

3. **Monitor Logs**:
```bash
# In Render dashboard, check logs for security events
# Look for authentication failures and suspicious patterns
```

## 🔒 Step 9: SSL/TLS Configuration

Render automatically provides HTTPS, but verify:

1. **Frontend calls backend over HTTPS**
2. **WebSocket connections use WSS**
3. **CORS allows secure origins only**

Update frontend WebSocket connection:

```typescript
// In networkUtils.ts
export const getWebSocketUrl = (): string => {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${wsProtocol}//${window.location.host}`;
};
```

## ✅ Final Deployment Commands

1. **Deploy Backend**:
```bash
git add .
git commit -m "Add JWT security configuration for Render"
git push origin main
```

2. **Verify Deployment**:
```bash
# Check health endpoint
curl https://your-backend.onrender.com/actuator/health

# Test authentication
curl -X POST https://your-backend.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

## 📝 Environment Variables Summary

**Required for Render:**

| Variable | Value | Source |
|----------|-------|--------|
| `JWT_SECRET` | `<generated-secret>` | Manual (generated script) |
| `JWT_EXPIRATION` | `14400000` | Manual |
| `JWT_ISSUER` | `chat-platform-backend` | Manual |
| `JWT_AUDIENCE` | `chat-platform-users` | Manual |
| `DATABASE_URL` | `<postgres-url>` | Render auto-generated |
| `MONGODB_URI` | `<mongodb-url>` | Render auto-generated |
| `REDIS_URL` | `<redis-url>` | Render auto-generated |
| `CORS_ALLOWED_ORIGINS` | `https://frontend.onrender.com` | Manual |

## 🎯 Success Checklist

After deployment, verify:

- [ ] JWT tokens are generated with proper claims
- [ ] Token blacklisting works on logout
- [ ] Issuer/audience validation prevents token reuse
- [ ] Frontend stores tokens securely
- [ ] CORS is configured properly
- [ ] All security logs are visible in Render dashboard
- [ ] Health checks pass
- [ ] WebSocket connections work over WSS

Your chat platform is now deployed with enterprise-grade JWT security on Render! 🎉
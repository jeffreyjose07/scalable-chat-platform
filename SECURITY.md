# 🔒 JWT Security Implementation Guide

This document outlines the security measures implemented in the Chat Platform to ensure secure JWT authentication.

## 🛡️ Security Features Implemented

### 1. **Strong JWT Configuration**
- ✅ **HS256 Algorithm**: Uses HMAC SHA256 signing (no `alg: "none"` vulnerability)
- ✅ **Environment-Based Secrets**: JWT secret configurable via `JWT_SECRET` environment variable
- ✅ **Issuer/Audience Claims**: Validates `iss` and `aud` claims to prevent token reuse
- ✅ **JWT ID (jti)**: Unique token identification for blacklisting capabilities
- ✅ **Reduced Expiry**: Token lifetime reduced to 4 hours (from 24 hours)

### 2. **Token Blacklisting System**
- ✅ **Redis-Based Blacklisting**: Secure token revocation using Redis storage
- ✅ **Automatic TTL**: Blacklisted tokens automatically expire with original token
- ✅ **Secure Logout**: Tokens are blacklisted on logout to prevent reuse
- ✅ **Authentication Filter Integration**: All requests check token blacklist status

### 3. **Secure Frontend Storage**
- ✅ **SessionStorage Priority**: Uses sessionStorage by default (cleared on tab close)
- ✅ **Token Encryption**: Basic XOR encryption for stored tokens in production
- ✅ **Browser Fingerprinting**: Detects environment changes for additional security
- ✅ **Auto-Cleanup**: Tokens cleared on page unload and error events
- ✅ **Age Validation**: Tokens older than 24 hours are automatically removed

### 4. **Comprehensive Validation**
- ✅ **Claims Validation**: Validates `exp`, `iat`, `iss`, `aud`, and `jti` claims
- ✅ **Blacklist Checking**: Every authentication checks token blacklist status
- ✅ **Exception Handling**: Graceful error handling with security-first approach

## 🚀 Production Deployment

### Environment Variables

Set these environment variables for production:

```bash
# Generate a strong JWT secret (256-bit)
JWT_SECRET=$(openssl rand -base64 32)

# JWT Configuration
JWT_EXPIRATION=14400000  # 4 hours in milliseconds
JWT_ISSUER=chat-platform-backend
JWT_AUDIENCE=chat-platform-users

# Redis Configuration (for token blacklisting)
SPRING_REDIS_HOST=your-redis-host
SPRING_REDIS_PORT=6379
```

### Generate JWT Secret

Use the provided script to generate a cryptographically secure JWT secret:

```bash
./scripts/generate-jwt-secret.sh
```

### Docker Deployment

```dockerfile
# Add to your Dockerfile environment
ENV JWT_SECRET=${JWT_SECRET}
ENV JWT_EXPIRATION=14400000
ENV JWT_ISSUER=chat-platform-backend
ENV JWT_AUDIENCE=chat-platform-users
```

### Kubernetes Deployment

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
type: Opaque
data:
  JWT_SECRET: <base64-encoded-secret>
---
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: chat-platform
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: JWT_SECRET
```

## 🔧 Configuration Details

### Backend Configuration (`application.yml`)

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:CHANGE_IN_PRODUCTION}
    expiration: ${JWT_EXPIRATION:14400000} # 4 hours
    issuer: ${JWT_ISSUER:chat-platform-backend}
    audience: ${JWT_AUDIENCE:chat-platform-users}
```

### Frontend Configuration (`secureStorage.ts`)

```typescript
// Secure token storage with encryption
export const tokenStorage = {
  set: (token: string, persistent: boolean = false) => secureStorage.setToken(token, persistent),
  get: () => secureStorage.getToken(),
  remove: () => secureStorage.removeToken(),
  exists: () => secureStorage.hasToken(),
  clear: () => secureStorage.clearAll()
};
```

## 🛠️ Security Components

### 1. **JwtService** (`JwtService.java`)
- Token generation with all security claims
- Comprehensive validation with blacklist integration
- Issuer/audience validation

### 2. **TokenBlacklistService** (`TokenBlacklistService.java`)
- Redis-based token blacklisting
- Automatic cleanup of expired tokens
- Security incident response capabilities

### 3. **JwtAuthenticationFilter** (`JwtAuthenticationFilter.java`)
- Request interception and token validation
- Blacklist checking on every request
- Graceful error handling

### 4. **SecureStorage** (`secureStorage.ts`)
- Client-side token encryption
- Browser fingerprinting
- Automatic cleanup mechanisms

## 📊 Security Monitoring

### Metrics to Monitor

1. **Token Blacklist Size**: Monitor Redis key count for blacklisted tokens
2. **Authentication Failures**: Track failed JWT validations
3. **Token Age**: Monitor token creation and usage patterns
4. **Storage Security**: Track secure storage usage and errors

### Logging

The system logs security events at appropriate levels:

- **INFO**: Successful authentications, token generation
- **WARN**: Token validation failures, security anomalies
- **ERROR**: Authentication system errors, critical security events
- **DEBUG**: Detailed security flow information

## 🚨 Security Incident Response

### Token Compromise Response

1. **Immediate Action**:
   ```bash
   # Blacklist specific token
   redis-cli SET "jwt:blacklist:<jti>" "compromised" EX <ttl_seconds>
   ```

2. **Generate New Secret**:
   ```bash
   ./scripts/generate-jwt-secret.sh
   # Deploy new secret to invalidate all existing tokens
   ```

3. **Monitor Logs**:
   ```bash
   # Check for suspicious authentication attempts
   tail -f logs/security.log | grep "authentication failed"
   ```

### Best Practices

1. **Rotate JWT Secrets**: Every 3-6 months or after security incidents
2. **Monitor Token Usage**: Track unusual authentication patterns
3. **Regular Security Audits**: Review token storage and validation logic
4. **Update Dependencies**: Keep JWT libraries and dependencies current
5. **Test Security Features**: Regular penetration testing of authentication flows

## 🔍 Security Validation Checklist

- [ ] JWT secret is cryptographically strong (256+ bits)
- [ ] No sensitive data stored in JWT payload
- [ ] Token expiration is reasonable (≤ 4 hours)
- [ ] Issuer and audience claims are validated
- [ ] Token blacklisting is functional
- [ ] Frontend uses secure storage (sessionStorage preferred)
- [ ] All authentication endpoints require proper validation
- [ ] Security logs are monitored
- [ ] Backup authentication methods are available

## 📚 Additional Resources

- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [Spring Security JWT Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

---

## 🚨 GitGuardian Secret Detection & Remediation

### Hardcoded Secrets Prevention

This project is protected by GitGuardian to prevent hardcoded secrets in the codebase:

#### ✅ **What We Fixed**
1. **application-render.yml**: Removed default JWT secret and admin password values
2. **Test Files**: Updated test passwords to avoid generic password detection  
3. **Environment Configuration**: All secrets now require environment variables

#### 🔒 **Mandatory Environment Variables**
The following environment variables MUST be set in production:

```bash
# JWT Security (CRITICAL - No defaults provided)
JWT_SECRET=<your-256-bit-secret-key>
JWT_EXPIRATION=14400000
JWT_ISSUER=chat-platform-backend  
JWT_AUDIENCE=chat-platform-users

# Admin Account (CRITICAL - No defaults provided)
ADMIN_USERNAME=<secure-admin-username>
ADMIN_EMAIL=<admin-email>
ADMIN_PASSWORD=<secure-admin-password>

# Database Configuration
DATABASE_URL=<postgresql-connection-url>
DATABASE_USERNAME=<db-username>
DATABASE_PASSWORD=<secure-db-password>
MONGODB_URI=<mongodb-connection-uri>
REDIS_URL=<redis-connection-url>
```

#### 🛡️ **Secret Generation Best Practices**

**JWT Secret Generation:**
```bash
# Generate cryptographically secure JWT secret
openssl rand -base64 64
# OR
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

**Password Requirements:**
- Minimum 12 characters for admin passwords
- Include uppercase, lowercase, numbers, and special characters
- Use password managers for generation and storage

#### ⚠️ **Security Incident Response**

If secrets are accidentally committed:

1. **Immediate Actions**:
   - Rotate ALL affected secrets immediately
   - Update production environment variables
   - Review access logs for unauthorized usage

2. **Git History Cleanup**:
   ```bash
   # Remove secrets from git history (DANGEROUS - coordinate with team)
   git filter-branch --force --index-filter \
   'git rm --cached --ignore-unmatch <file-with-secrets>' \
   --prune-empty --tag-name-filter cat -- --all
   
   # Force push to update remote (coordinate with team first)
   git push origin --force --all
   ```

3. **Prevention Setup**:
   ```bash
   # Install GitGuardian pre-commit hook
   pip install detect-secrets
   pre-commit install
   ```

#### 📋 **Configuration File Security Status**

| File | Security Status | Notes |
|------|----------------|--------|
| `application.yml` | ✅ Secure | Uses env variables only |
| `application-render.yml` | ✅ Fixed | Removed hardcoded defaults |
| `application-docker.yml` | ✅ Secure | Uses env variables only |
| `application-test.yml` | ✅ Secure | Test-only mock values |
| `.env.example` | ✅ Secure | Template with placeholders |

#### 🔍 **Pre-Commit Security Checks**

The project includes pre-commit hooks that scan for:
- Hardcoded passwords and API keys
- JWT secrets and tokens
- Database connection strings
- Email addresses in configuration
- Common secret patterns

#### 📚 **Environment Setup Guide**

1. **Copy Environment Template**:
   ```bash
   cp .env.example .env
   ```

2. **Generate Required Secrets**:
   ```bash
   # Generate JWT secret
   echo "JWT_SECRET=$(openssl rand -base64 64)" >> .env
   
   # Set other required variables
   echo "ADMIN_USERNAME=your_admin_username" >> .env
   echo "ADMIN_PASSWORD=$(openssl rand -base64 32)" >> .env
   ```

3. **Validate Configuration**:
   ```bash
   # Ensure no defaults are used in production
   grep -r "CHANGE_THIS\|admin123\|password123" src/ || echo "✅ No hardcoded secrets found"
   ```

---

**Security Notice**: This implementation follows industry best practices for JWT security and secret management. All secrets must be provided via environment variables. Regular security audits and GitGuardian monitoring help maintain security posture.
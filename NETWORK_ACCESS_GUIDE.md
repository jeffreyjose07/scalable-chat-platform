# Network Access Guide

This guide explains how to access the chat application from other machines on the same network.

## ✅ **NEW: Automatic IP Detection**

The application now **automatically detects your current IP** and configures API endpoints accordingly. No more hardcoded IPs!

## How It Works

### **Frontend (Automatic Detection)**
- When accessed via `localhost` → Uses `localhost:8080` for API
- When accessed via local IP (e.g., `192.168.1.100`) → Uses same IP for API
- WebSocket connections automatically match the API configuration

### **Backend (Dynamic CORS)**
- Automatically allows any private network IP ranges:
  - `192.168.x.x:3000/3001`
  - `10.x.x.x:3000/3001` 
  - `172.16-31.x.x:3000/3001`
- Always allows `localhost` and `127.0.0.1`

## 🚀 Usage Instructions

### **For Network Access:**
```bash
# 1. Start infrastructure
./start-dev.sh

# 2. Start backend
cd backend && mvn spring-boot:run

# 3. Start frontend with network access
cd frontend && npm run start:network
```

### **For Localhost Only:**
```bash
# 1. Start infrastructure
./start-dev.sh

# 2. Start backend
cd backend && mvn spring-boot:run

# 3. Start frontend (localhost only)
cd frontend && npm start
```

### **Access From Any Machine:**
1. **Find your machine's IP**: 
   ```bash
   # macOS/Linux
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # Windows
   ipconfig | findstr IPv4
   ```

2. **Access via**: `http://YOUR_IP:3000`
3. **Login/registration will work automatically** 🎉
4. **No configuration needed** - Everything is auto-detected!

## 🌟 Features

### **🔍 Network Debug Info**
Click the "Network Info" button in the chat window to see:
- 📍 **Current frontend URL** and port
- 🔗 **Detected API and WebSocket URLs**
- 🌐 **IP address detection** status
- ⚙️ **Real-time configuration** updates
- 📊 **Network performance** metrics
- 🛡️ **Security validation** results

### **🔄 No More IP Hardcoding**
- ✅ **Works on any WiFi network** automatically
- ✅ **Adapts when IP changes** during development
- ✅ **No configuration needed** for new networks
- ✅ **Survives router restarts** and IP reassignments
- ✅ **Multiple network interfaces** supported

### **🛡️ Secure by Design**
- ✅ **Only allows private network ranges** (RFC 1918)
- ✅ **Blocks external internet IPs** for security
- ✅ **Maintains localhost access** for development
- ✅ **CORS protection** with dynamic origin validation
- ✅ **JWT token validation** for all API requests
- ✅ **Input sanitization** and validation

## ⚙️ Configuration Override

If you need to override automatic detection, set environment variables:

### **`.env.local`** (for overrides)
```bash
# Force specific endpoints
REACT_APP_API_URL=http://your-custom-ip:8080
REACT_APP_WS_URL=ws://your-custom-ip:8080

# Debug mode
REACT_APP_DEBUG_NETWORK=true

# Custom port
PORT=3001
```

### **Backend Configuration** (application.yml)
```yaml
server:
  port: 8080
  
cors:
  allowed-origins:
    - http://localhost:3000
    - http://localhost:3001
  custom-ip-ranges:
    - 192.168.1.0/24
    - 10.0.0.0/8
```

### **Environment-Specific Overrides**
```bash
# Development
SPRING_PROFILES_ACTIVE=dev

# Production
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

## Troubleshooting

### **If Login Still Fails:**

1. **Check Network Debug Info** (button in chat window)
2. **Verify API URLs** are pointing to correct IP
3. **Check browser console** for CORS errors
4. **Test backend directly**: `curl http://YOUR_IP:8080/api/health`

### **Common Issues:**

**🔥 Firewall Issues:**
- **Ports 3000/8080 blocked** → Configure firewall to allow these ports
- **Backend not accessible** → Check system firewall settings
- **macOS**: System Preferences > Security & Privacy > Firewall
- **Windows**: Windows Defender Firewall > Allow an app

**📶 Network Issues:**
- **Use `npm run start:network`** not regular `npm start`
- **Check IP range** - Must be private network (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
- **VPN interference** - Disable VPN if causing issues
- **Router isolation** - Some routers block device-to-device communication

**🔧 Debug Steps:**
1. **Test backend directly**: `curl http://YOUR_IP:8080/api/health`
2. **Check network info**: Click "Network Info" button in chat
3. **Verify IP detection**: Look for auto-detected URLs in frontend
4. **Check browser console**: Look for CORS or connection errors

## 🔍 Technical Details

### **Private IP Ranges Supported:**
- `192.168.0.0/16` - Most home networks (65,536 addresses)
- `10.0.0.0/8` - Corporate networks (16,777,216 addresses)
- `172.16.0.0/12` - Docker/VPN networks (1,048,576 addresses)
- `127.0.0.1/localhost` - Local development

### **How Detection Works:**
1. **Frontend**: Checks `window.location.hostname`
2. **IP Pattern Matching**: Validates against private IP regex patterns
3. **API URL Construction**: Uses detected IP for backend endpoints
4. **WebSocket Configuration**: Follows same IP detection logic
5. **Backend Validation**: Dynamically validates CORS origins
6. **Fallback Mechanism**: Defaults to localhost if detection fails

### **Code Implementation:**
```typescript
// Frontend IP Detection
const isLocalIP = (ip: string): boolean => {
  return /^(192\.168\.|10\.|172\.(1[6-9]|2[0-9]|3[01])\.)/.test(ip);
};

// Backend CORS Configuration
@Bean
public CorsConfiguration corsConfiguration() {
  return new DynamicCorsConfiguration()
    .allowPrivateNetworks()
    .allowWebSocketUpgrade();
}
```

## 🔄 Migration from Old Hardcoded Setup

### **Backward Compatibility**
The new system is **100% backward compatible**:
- ✅ **Old `.env` files** still work
- ✅ **Environment variables** still override detection
- ✅ **Gradual migration** possible without breaking existing setups
- ✅ **Legacy IP configurations** supported

### **Migration Steps**
1. **Remove hardcoded IPs** from `.env` files (optional)
2. **Use `npm run start:network`** instead of `npm start`
3. **Test auto-detection** using "Network Info" button
4. **Keep `.env.local`** for custom overrides if needed

### **Benefits of Migration**
- ✅ **No more IP updates** when changing networks
- ✅ **Automatic CORS configuration**
- ✅ **Better error handling** and debugging
- ✅ **Consistent network behavior**
- ✅ **Future-proof** for new network environments

---

## 🛡️ Security Features

### **Network Security**
- **Private IP only** - Blocks external internet access
- **CORS protection** - Only allows configured origins
- **JWT authentication** - Secure token-based auth
- **Input validation** - Prevents injection attacks

### **Supported IP Ranges**
- `192.168.0.0/16` - Home networks
- `10.0.0.0/8` - Corporate networks
- `172.16.0.0/12` - Docker/container networks
- `127.0.0.1/localhost` - Local development

### **Blocked by Design**
- Public IP addresses
- External internet domains
- Unsecured protocols
- Cross-origin requests from unknown sources

---

## 📊 Performance Optimization

### **Network Performance**
- **Automatic IP detection** - No DNS lookups needed
- **WebSocket persistence** - Maintains connections efficiently
- **Local network routing** - Direct device-to-device communication
- **Caching strategy** - Redis for session management

### **Monitoring Tools**
- **Network Info button** - Real-time network status
- **Browser developer tools** - Network tab for debugging
- **Backend health endpoint** - `/api/health` for service status
- **WebSocket status** - Connection state in UI

---

**🎉 No more IP configuration headaches!**
**🚀 Works seamlessly across different networks!**
**🔒 Secure by design with private network restrictions!**
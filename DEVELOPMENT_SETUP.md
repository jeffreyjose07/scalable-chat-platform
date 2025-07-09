# Development Setup Guide

## 🚀 Quick Start

### **Using the Start Script (Recommended)**
```bash
# Start all infrastructure services and create Kafka topics
./start-dev.sh

# Start backend (in a new terminal)
cd backend && mvn spring-boot:run

# Start frontend (in a new terminal)
cd frontend && npm install && npm run start:network
```

### **Manual Setup**
```bash
# Start infrastructure
docker-compose up -d

# Wait for services to be ready
sleep 15

# Create Kafka topic manually
docker exec scalable-chat-platform-kafka-1 kafka-topics --create --topic chat-messages --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3

# Start backend
cd backend && mvn spring-boot:run

# Start frontend
cd frontend && npm install && npm run start:network
```

## 📋 Services Overview

| Service | Port | Health Check |
|---------|------|--------------|
| **Frontend** | 3000 | http://localhost:3000 |
| **Backend** | 8080 | http://localhost:8080/api/health |
| **PostgreSQL** | 5432 | `docker-compose ps postgres` |
| **MongoDB** | 27017 | `docker-compose ps mongodb` |
| **Redis** | 6379 | `docker-compose ps redis` |
| **Kafka** | 9092 | `docker exec scalable-chat-platform-kafka-1 kafka-topics --list --bootstrap-server localhost:9092` |
| **Zookeeper** | 2181 | `docker-compose ps zookeeper` |
| **Elasticsearch** | 9200 | http://localhost:9200 |

## 🔧 Kafka Topic Management

### **Why Topics Get "Deleted"**

Topics don't actually get deleted, but they become inaccessible when:
1. **Volumes are removed**: `docker-compose down -v` removes all data
2. **Cluster ID mismatch**: Kafka creates new cluster IDs on fresh starts (FIXED: Added persistent Zookeeper volumes)
3. **Container recreation**: New containers lose previous topic data

**Root Cause Fixed**: The main issue was that Zookeeper didn't have persistent volumes, so it generated new cluster IDs on each restart while Kafka retained old cluster IDs. This has been resolved by adding `zookeeper_data` and `zookeeper_log` volumes.

### **Topic Auto-Creation**

The application now includes **multiple layers** of topic creation:

1. **Startup Script**: `./start-dev.sh` creates topics after Kafka is ready
2. **Java Configuration**: `KafkaTopicConfig.java` defines topics as Spring beans
3. **Startup Validator**: `KafkaTopicValidator.java` validates/creates topics on app start
4. **Kafka Health Service**: `KafkaHealthService.java` monitors and recreates topics every 30 seconds

### **Verify Topics Exist**
```bash
# List all topics
docker exec scalable-chat-platform-kafka-1 kafka-topics --list --bootstrap-server localhost:9092

# Check specific topic details
docker exec scalable-chat-platform-kafka-1 kafka-topics --describe --topic chat-messages --bootstrap-server localhost:9092
```

### **Manual Topic Creation**
```bash
# Create the chat-messages topic
docker exec scalable-chat-platform-kafka-1 kafka-topics --create --topic chat-messages --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3

# Verify creation
docker exec scalable-chat-platform-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
```

## 🐛 Troubleshooting

### **Messages Not Appearing in Real-time**

1. **Check Kafka is running**:
   ```bash
   docker ps | grep kafka
   ```

2. **Check topic exists**:
   ```bash
   docker exec scalable-chat-platform-kafka-1 kafka-topics --list --bootstrap-server localhost:9092
   ```

3. **Check backend logs** for:
   ```
   ✅ Message sent to Kafka successfully
   📨 Received message from Kafka
   🚀 Published MessageDistributionEvent
   📢 Received MessageDistributionEvent
   ```

4. **If topic missing**:
   ```bash
   # Recreate topic
   docker exec scalable-chat-platform-kafka-1 kafka-topics --create --topic chat-messages --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3
   ```

### **Kafka Cluster ID Mismatch**
```bash
# ✅ RECOMMENDED: Fix without data loss
./fix-kafka-only.sh

# ⚠️ DESTRUCTIVE: Only if you want to reset everything
docker-compose down -v && ./start-dev.sh
```

**🚨 Important**: Always use `./fix-kafka-only.sh` to preserve user data and messages!

### **Network Access Issues**
```bash
# Use network script for external access
cd frontend && npm run start:network

# Check network configuration in chat (click "Network Info" button)
```

## 📁 Important Files

### **Infrastructure**
- `docker-compose.yml` - Service definitions
- `start-dev.sh` - Development startup script
- `stop-dev.sh` - Development shutdown script

### **Backend Kafka Configuration**
- `backend/src/main/java/com/chatplatform/config/KafkaTopicConfig.java` - Topic definitions
- `backend/src/main/java/com/chatplatform/config/KafkaTopicValidator.java` - Topic validation
- `backend/src/main/java/com/chatplatform/service/KafkaHealthService.java` - Health monitoring
- `backend/src/main/java/com/chatplatform/service/MessageService.java` - Message processing

### **Frontend Configuration**
- `frontend/src/utils/networkUtils.ts` - Network detection
- `frontend/src/components/NetworkDebug.tsx` - Network info display

## 🔄 Development Workflow

### **Start Development**
```bash
./start-dev.sh
# Wait for "✅ Topic chat-messages already exists" or "✅ Topic created successfully"
```

### **Start Backend**
```bash
cd backend && mvn spring-boot:run
# Look for "✅ Kafka topic validation successful"
```

### **Start Frontend**
```bash
cd frontend && npm run start:network
# For network access from other machines
```

### **Stop Development**
```bash
./stop-dev.sh
# This preserves volumes and topics
```

## 📝 Logs to Monitor

### **Backend Startup**
```
🔧 Initializing Kafka topics configuration...
📍 Kafka bootstrap servers: localhost:9092
📝 Chat messages topic will be created with 3 partitions and 1 replica
✅ Kafka topic validation successful
🌐 Dynamic CORS configured for private IP ranges
```

### **Message Flow**
```
✅ Message sent to Kafka successfully: [id] (partition: 0, offset: 1)
📨 Received message from Kafka: [id] (content: hello...)
🚀 Published MessageDistributionEvent for message: [id]
📢 Received MessageDistributionEvent for message: [id]
[WS-BROADCAST] Broadcasting message [id] to sessions: [session-list]
```

### **Network Access**
```
🌐 Detected IP: 192.168.1.100
✅ API URL configured: http://192.168.1.100:8080
✅ WebSocket URL configured: ws://192.168.1.100:8080
📡 WebSocket connected successfully
```

### **Error Patterns**
```
❌ Kafka cluster ID mismatch detected
⚠️ Topic 'chat-messages' not found, creating...
🔄 Retrying WebSocket connection...
🚨 CORS blocked: Origin not in allowed list
```

## 🎯 Key Points

1. **Always use `./start-dev.sh`** - Ensures topics are created and health validated
2. **Use `./stop-dev.sh`** - Preserves all data including users and messages
3. **Use `./fix-kafka-only.sh`** - Fixes Kafka issues without data loss
4. **Avoid `docker-compose down -v`** - Unless you want to reset everything
5. **Monitor backend logs** - Look for emoji indicators for status
6. **Use `npm run start:network`** - For network access from other devices
7. **Click "Network Info"** - In chat window to debug network issues
8. **Persistent volumes** - User data survives service restarts
9. **Auto-detection** - IP and CORS configuration happens automatically
10. **Multi-layer validation** - Topics are created/validated at multiple levels

**With this setup, topics should never be missing again! 🚀**

---

## 🛡️ Data Protection

### **Preserve User Data**
```bash
# ✅ SAFE: Stops services, preserves data
./stop-dev.sh

# ✅ SAFE: Fixes Kafka issues only
./fix-kafka-only.sh

# ⚠️ DESTRUCTIVE: Removes all data including users/messages
docker-compose down -v
```

### **Volume Management**
- **User data**: `postgres_data` (authentication, user profiles)
- **Messages**: `mongodb_data` (chat history, conversations)
- **Sessions**: `redis_data` (active connections, cache)
- **Kafka data**: `kafka_data` (message queue, offsets)
- **Zookeeper**: `zookeeper_data`, `zookeeper_log` (cluster coordination)
- **Search**: `elasticsearch_data` (message search index)

### **Recovery Procedures**

**If topics are missing**:
```bash
./start-dev.sh
# Auto-creates topics and validates health
```

**If cluster ID mismatch**:
```bash
./fix-kafka-only.sh
# Removes only Kafka/Zookeeper volumes, preserves user data
```

**If complete reset needed**:
```bash
docker-compose down -v
./start-dev.sh
# ⚠️ This will delete all user accounts and messages
```
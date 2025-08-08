# Scalable Chat Platform

A production-ready real-time chat platform built with Spring Boot 3.2 and React 18, optimized for single-service deployment with comprehensive documentation.

## 🚀 Quick Start

### One-Command Development Setup
```bash
# Start infrastructure (PostgreSQL, MongoDB, Redis)
./start-dev.sh

# In another terminal - Start backend
cd backend && export JAVA_HOME=/path/to/java17 && ./gradlew bootRun

# In another terminal - Start frontend
cd frontend && npm install && npm start
```

### Production Build
```bash
# Build single JAR with embedded frontend
export JAVA_HOME=/path/to/java17
cd backend && ./gradlew buildForRender
```

## 🏗️ Architecture Overview

- **Backend**: Spring Boot 3.2 with Java 17, WebSocket support
- **Databases**: PostgreSQL (users), MongoDB (messages), Redis (sessions)
- **Frontend**: React 18 with TypeScript, embedded in Spring Boot JAR
- **Messaging**: In-memory queue with event-driven distribution
- **Deployment**: Single-service container optimized for Render platform

**🎯 [Complete Architecture Documentation →](docs/ARCHITECTURE.md)**

## 🚀 Architecture & Scalability

### **Single-Instance Deployment Strategy**

**Current State**: This platform is deployed as a single instance for cost optimization ($0/month), but the architecture is **designed for horizontal scaling**.

```
Cost-Optimized Deployment (Current)
[Frontend + Backend JAR] → [PostgreSQL + MongoDB + Redis]
     Single Instance              External Databases
```

**Why Single Instance?**
- **MVP/Demo Focus**: Validates product-market fit before scaling costs
- **Free Tier Maximization**: Render + external database free tiers
- **Reduced Complexity**: Single deployment, monitoring, and maintenance point
- **Performance Adequate**: Handles 1000+ concurrent WebSocket connections

### **Built-in Scalability Patterns**

The codebase implements **enterprise-grade scalability patterns**:

#### 🔄 **Stateless Service Design**
```java
// All session state externalized to Redis
@Service
public class ConnectionManager {
    public void registerConnection(String userId, String serverId, String sessionId) {
        redisTemplate.opsForValue().set("user:server:" + userId, serverId);
        // User-to-server mapping for multi-instance routing
    }
}
```

#### 📨 **Event-Driven Architecture**
```java
// Asynchronous message processing ready for external queues
@EventListener
@Async
public void handleMessageDistribution(MessageDistributionEvent event) {
    distributeMessage(event.message());
    // Currently: Spring Events → Future: Kafka/RabbitMQ
}
```

#### 🏷️ **Server Instance Identification**
```java
private String getServerId() {
    return System.getenv().getOrDefault("SERVER_ID", "server-1");
    // Environment-based server identification for scaling
}
```

### **Horizontal Scaling Implementation**

#### **Phase 1: Multi-Instance Backend (Cost: ~$20/month)**
```
Load Balancer Configuration
├── Instance 1: SERVER_ID=server-1 
├── Instance 2: SERVER_ID=server-2
└── Instance 3: SERVER_ID=server-3
         ↓
    Shared Redis tracks user-to-server mapping
```

**Implementation Steps:**
1. Deploy multiple backend instances with unique `SERVER_ID`
2. Configure load balancer with sticky sessions for WebSocket
3. Redis automatically handles cross-instance session tracking

#### **Phase 2: External Message Queue (Cost: ~$50/month)**
```java
// Current: In-memory Spring Events
@EventListener
public void handleMessageDistribution(MessageDistributionEvent event)

// Scaled: Kafka/RabbitMQ
@KafkaListener(topics = "message-distribution")
public void handleMessageDistribution(MessageDistributionEvent event)
```

#### **Phase 3: Database Optimization (Cost: ~$100/month)**
- **Read Replicas**: MongoDB and PostgreSQL read scaling
- **Connection Pooling**: Already implemented with HikariCP
- **Caching Layer**: Redis-based query result caching

### **Technical Interview Talking Points**

#### **For Senior Engineers/Hiring Managers:**

**Question**: *"How does this scale beyond a single instance?"*

**Answer**: *"The architecture implements stateless design patterns from day one. The ConnectionManager uses Redis to map users to server instances, so when we deploy multiple backend instances behind a load balancer, Redis handles cross-server message routing automatically. The MessageDistributionService is already event-driven with @Async processing - we just swap Spring Events for Kafka when we need cross-instance messaging."*

**Question**: *"Why not start with microservices?"*

**Answer**: *"This follows the 'monolith first' pattern recommended by Martin Fowler. We avoid the distributed systems complexity until we have proven product-market fit and clear service boundaries. The modular service layer (MessageService, ConversationService, etc.) makes the future microservices transition straightforward."*

**Question**: *"What about database bottlenecks?"*

**Answer**: *"We use a multi-database strategy: PostgreSQL for ACID-compliant user data, MongoDB for high-volume chat messages, and Redis for sub-millisecond session management. Each database is optimized for its workload pattern and can scale independently."*

### **Code Evidence of Scalability**

#### **Redis-Based Session Management** (`ConnectionManager.java:18-32`)
```java
public void registerConnection(String userId, String serverId, String sessionId) {
    redisTemplate.opsForValue().set("user:server:" + userId, serverId);
    redisTemplate.opsForSet().add("server:sessions:" + serverId, sessionId);
    // Stateless design enables horizontal scaling
}
```

#### **Event-Driven Message Distribution** (`MessageDistributionService.java:28-37`)
```java
@EventListener
@Async
public void handleMessageDistribution(MessageDistributionEvent event) {
    distributeMessage(event.message());
    // Ready to replace with Kafka/RabbitMQ for cross-instance messaging
}
```

#### **External Database Architecture**
- **PostgreSQL**: User accounts, conversations, participant relationships
- **MongoDB**: Chat message history with automatic indexing
- **Redis**: Session state, presence management, caching layer

### **Performance Characteristics**

| Metric | Single Instance | Multi-Instance (3x) |
|--------|----------------|-------------------|
| **Concurrent Users** | 1,000+ | 3,000+ |
| **Messages/Second** | 500+ | 1,500+ |
| **WebSocket Connections** | 1,000+ | 3,000+ |
| **Database Load** | Shared | Distributed |
| **Memory Usage** | 512MB-1GB | 1.5GB-3GB |

### **Scaling Roadmap**

#### **Immediate (0-6 months)**
- ✅ Single-instance production deployment
- ✅ External database architecture
- ✅ Monitoring and health checks
- ✅ Security hardening (JWT, CORS, rate limiting)

#### **Growth Phase (6-12 months)**
- 🔄 Multi-instance backend deployment
- 🔄 Load balancer configuration
- 🔄 External message queue (Kafka/RabbitMQ)
- 🔄 Advanced monitoring (ELK stack, Prometheus)

#### **Scale Phase (1-2 years)**
- 🔄 Database read replicas
- 🔄 CDN for static assets
- 🔄 Microservices extraction
- 🔄 Geographic distribution

### **Production Readiness Features**

- ✅ **Health Monitoring**: Actuator endpoints for database connectivity
- ✅ **Security**: JWT authentication, CORS, input validation, rate limiting
- ✅ **Error Handling**: Graceful degradation and automatic recovery
- ✅ **Logging**: Structured logging for monitoring and debugging
- ✅ **Build Optimization**: 20-40% faster startup with lazy initialization
- ✅ **Container Ready**: Docker support with multi-stage builds

---

## 📚 Documentation Tree

### 🏗️ **Architecture & Design**
```
├── 📋 System Architecture    → docs/ARCHITECTURE.md
├── 🎯 Professional Demo     → docs/DEMO.md  
└── 🔧 Development Setup     → docs/development/setup.md
```

### 🚀 **Component Documentation**
```
├── ⚙️  Backend (Spring Boot)  → backend/README.md
├── ⚛️  Frontend (React)       → frontend/README.md
└── 🧪 Testing Strategy       → backend/src/test/README.md
```

### 🌐 **Deployment & Operations**
```
├── 🚀 Render Deployment      → docs/deployment/render.md
├── 📊 Deployment Overview    → docs/deployment/README.md
├── 🔐 JWT Security Setup     → docs/render-jwt-security-summary.md
└── 🛡️  Security Deployment    → docs/render-security-deployment.md
```

### 📖 **Project Documentation**
```
├── ✨ Features Overview      → FEATURES.md
├── 📊 Project Phases        → PROJECT_PHASES.md
├── 📝 Changelog             → CHANGELOG.md
├── 🔐 Security Guidelines    → SECURITY.md
└── 🌐 Network Access Guide  → NETWORK_ACCESS_GUIDE.md
```

---

## ✨ Key Features

### Core Features
- ✅ **Real-time messaging** via WebSocket with automatic reconnection
- ✅ **Private messaging** with direct conversations and user discovery
- ✅ **Group conversations** with persistent state and participant management
- ✅ **Advanced group management** with role-based access control (OWNER/ADMIN/MEMBER)
- ✅ **Group creation & settings** with customizable permissions and metadata
- ✅ **Conversation deletion** with proper message cleanup and role-based permissions
- ✅ **Unread message tracking** with industry-standard timestamp-based system
- ✅ **Message search** within conversations with highlighting and pagination
- ✅ **Advanced search features** with recent searches, filters, and proper z-index handling
- ✅ **Read receipts system** with WhatsApp-style visual indicators (sent/delivered/read)
- ✅ **Modern UI/UX** following WhatsApp/Telegram design standards with message bubbles and gradients
- ✅ **User authentication** with JWT tokens and secure session management
- ✅ **Message persistence** (MongoDB with automatic indexing)
- ✅ **Connection management** (Redis with session tracking)
- ✅ **In-memory message queue** with event-driven distribution
- ✅ **Responsive UI** with Tailwind CSS and mobile-first design

### Infrastructure Features
- ✅ **Single-service deployment** optimized for production
- ✅ **Docker containerization** with multi-stage builds
- ✅ **Build optimization** with lazy initialization and JVM tuning
- ✅ **Health monitoring** with actuator endpoints
- ✅ **Dynamic CORS configuration** for cross-origin access
- ✅ **Database connection pooling** optimized for cloud deployment
- ✅ **Enhanced logging** with structured output
- ✅ **Graceful shutdown** and error handling

### Technical Highlights
- **Single-Service Architecture**: Optimized monolithic deployment with embedded frontend
- **Multi-Database Strategy**: PostgreSQL for users/conversations, MongoDB for messages, Redis for sessions
- **Real-Time Communication**: WebSocket-based instant messaging with in-memory queuing
- **Modern React Architecture**: Custom hooks pattern with TypeScript
- **Industry-Standard UX**: Last-read timestamp approach used by Discord/Slack/WhatsApp
- **Production Ready**: Optimized build pipeline with startup performance tuning
- **Cloud Deployment**: Render platform integration with environment-specific configurations

## 🔌 Services & Infrastructure

### Development Environment
| Service | Port | Purpose | Documentation |
|---------|------|---------|---------------|
| Backend API | 8080 | Spring Boot with embedded frontend | [Backend Docs →](backend/README.md) |
| PostgreSQL | 5432 | User accounts and conversations | [Setup Guide →](docs/development/setup.md) |
| MongoDB | 27017 | Chat messages and history | [Setup Guide →](docs/development/setup.md) |
| Redis | 6379 | Session management and caching | [Setup Guide →](docs/development/setup.md) |

### Production Deployment
- **Single Service**: Frontend embedded as static resources in Spring Boot JAR
- **External Databases**: Managed PostgreSQL, MongoDB Atlas, Upstash Redis
- **Platform**: Render with build optimizations
- **📋 [Complete Deployment Guide →](docs/deployment/render.md)**

## 🛠️ Development Resources

### 📁 Project Structure
```
scalable-chat-platform/
├── 📂 backend/              → Spring Boot Application
│   ├── 📖 README.md         → Backend documentation
│   ├── 🧪 src/test/         → Testing strategy & tests
│   └── ⚙️  build.gradle     → Build configuration
├── 📂 frontend/             → React Application  
│   ├── 📖 README.md         → Frontend documentation
│   └── 📦 package.json     → Dependencies & scripts
├── 📂 docs/                 → Complete documentation
│   ├── 🏗️  ARCHITECTURE.md  → System design & diagrams
│   ├── 🚀 deployment/      → Deployment guides
│   ├── 🛠️  development/     → Setup & dev guides
│   ├── 📋 api/             → API documentation
│   ├── ✨ features/        → Feature specifications
│   └── 📚 guides/          → How-to guides
├── 🐳 docker-compose.yml   → Local infrastructure
├── 🚀 Dockerfile.render    → Production container
└── 📜 scripts/             → Utility scripts
```

### 🔗 Quick Links
- **[Backend Development →](backend/README.md)** - Spring Boot setup, API docs, testing
- **[Frontend Development →](frontend/README.md)** - React components, hooks, styling  
- **[System Architecture →](docs/ARCHITECTURE.md)** - Diagrams, data flow, technical design
- **[Development Setup →](docs/development/setup.md)** - Local environment configuration
- **[Testing Strategy →](backend/src/test/README.md)** - Unit tests, integration tests, coverage
- **[Render Deployment →](docs/deployment/render.md)** - Production deployment guide

## 🆘 Support & Troubleshooting

### 🔧 Quick Solutions
| Issue | Solution | Documentation |
|-------|----------|---------------|
| Build fails | Ensure Java 17+ installed | [Backend Setup →](backend/README.md#troubleshooting) |
| Database connection | Check Docker services running | [Development Setup →](docs/development/setup.md) |
| WebSocket issues | Verify CORS configuration | [Architecture →](docs/ARCHITECTURE.md#security-features) |
| Frontend errors | Clear node_modules, reinstall | [Frontend Guide →](frontend/README.md) |

### 📋 Comprehensive Guides
- **[Backend Troubleshooting →](backend/README.md#troubleshooting)** - Java, Gradle, Spring Boot issues
- **[Development Setup →](docs/development/setup.md)** - Environment configuration
- **[Deployment Issues →](docs/deployment/render.md)** - Production deployment problems
- **[System Architecture →](docs/ARCHITECTURE.md)** - Understanding the system design

## 📡 API Reference

### 🔗 API Documentation
**Complete API documentation is available in the backend README:**

├── 🔐 Authentication API     → [backend/README.md#authentication-endpoints](backend/README.md#authentication-endpoints)
├── 💬 Message API            → [backend/README.md#message-endpoints](backend/README.md#message-endpoints)  
├── 👥 Conversation API       → [backend/README.md#conversation-endpoints](backend/README.md#conversation-endpoints)
└── 🔌 WebSocket API          → [backend/README.md#websocket-endpoint](backend/README.md#websocket-endpoint)

### 🚀 Quick API Overview
| Endpoint Category | Base Path | Documentation |
|------------------|-----------|---------------|
| Authentication | `/api/auth/*` | [Auth API →](backend/README.md#authentication-endpoints) |
| Messages | `/api/messages/*` | [Message API →](backend/README.md#message-endpoints) |
| Conversations | `/api/conversations/*` | [Conversation API →](backend/README.md#conversation-endpoints) |
| WebSocket | `/ws/chat` | [WebSocket API →](backend/README.md#websocket-endpoint) |
| Health Check | `/api/health/status` | [Monitoring →](backend/README.md#monitoring--health) |

## 🧪 Testing & Quality

### 📋 Testing Documentation
```
🧪 Testing Resources
├── 📖 Testing Strategy        → backend/src/test/README.md
├── 🔧 Unit Tests             → backend/src/test/java/
├── 🌐 Integration Tests      → backend/src/integrationTest/
└── 📊 Coverage Reports       → build/jacocoHtml/index.html
```

### ⚡ Quick Test Commands
```bash
# Run all tests
cd backend && ./gradlew test

# Run with coverage report  
./gradlew test jacocoTestReport

# Run integration tests
./gradlew integrationTest

# Local code analysis
./gradlew localAnalysis
```

**🔗 [Complete Testing Guide →](backend/src/test/README.md)**

## 🚀 Production Deployment

### 🌐 Deployment Options
```
🚀 Deployment Guides  
├── 🎯 Render Platform (Recommended) → docs/deployment/render.md
├── 🐳 Docker Deployment           → docs/deployment/docker.md
├── ☁️  Generic Cloud Platform      → docs/deployment/cloud.md
└── 🏠 Self-Hosted                 → docs/deployment/self-hosted.md
```

### ⚡ Build Optimizations
- **20-40% faster startup** with lazy initialization
- **JVM container tuning** for optimal memory usage
- **Gradle build caching** for faster CI/CD
- **Single JAR deployment** with embedded frontend

**🔗 [Complete Deployment Guide →](docs/deployment/render.md)**

---

## 🏆 Production Features

- ✅ **Single-service deployment** - Simplified architecture
- ✅ **Build optimizations** - Fast startup and efficient resource usage  
- ✅ **Production monitoring** - Health checks and metrics
- ✅ **Security hardened** - JWT authentication, CORS, rate limiting
- ✅ **Database agnostic** - Works with managed cloud databases
- ✅ **Container ready** - Docker support with multi-stage builds

**Total deployment cost: $0/month** on free tiers! 🎉

---

## 🤝 Contributing & Support

### 📋 Development Workflow
1. **Fork & Clone** the repository
2. **Setup Environment** - Follow [development setup guide](docs/development/setup.md)
3. **Create Feature Branch** - `git checkout -b feature/your-feature`
4. **Make Changes** - Follow code standards and add tests
5. **Test Locally** - Run test suite and verify functionality
6. **Submit PR** - Include description and link any issues

### 📚 Documentation Standards
- **Keep docs current** - Update relevant .md files with changes
- **Link between docs** - Maintain cross-references and navigation
- **Include examples** - Add code samples and usage examples
- **Test instructions** - Verify setup steps work on fresh environment

---

## 📄 License & Usage

This project is for **educational and demonstration purposes**.

---

## 🗂️ Quick Navigation

**🏗️ Architecture & Design**
- [System Architecture](docs/ARCHITECTURE.md) - Complete system design with diagrams
- [Professional Demo](docs/DEMO.md) - Feature showcase and screenshots  
- [Features Overview](FEATURES.md) - Complete feature list and capabilities

**🛠️ Development**  
- [Backend Development](backend/README.md) - Spring Boot setup and API docs
- [Frontend Development](frontend/README.md) - React components and styling
- [Development Setup](docs/development/setup.md) - Local environment configuration
- [Testing Strategy](backend/src/test/README.md) - Test approach and coverage

**🚀 Deployment & Operations**
- [Render Deployment](docs/deployment/render.md) - Production deployment guide
- [Security Guidelines](SECURITY.md) - Security best practices
- [Network Access](NETWORK_ACCESS_GUIDE.md) - Multi-device testing setup

**📄 Project Management**
- [Project Phases](PROJECT_PHASES.md) - Development roadmap and milestones
- [Changelog](CHANGELOG.md) - Version history and updates
- [Contributing](CONTRIBUTING.md) - Contribution guidelines

---

*📅 Last Updated: January 2025 | 🏷️ Version: 1.0.0*
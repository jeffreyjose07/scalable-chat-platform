# System Architecture

## Overview

The Scalable Chat Platform follows a microservices-inspired architecture with real-time messaging capabilities, designed for horizontal scalability and high availability.

## Architecture Diagram

```mermaid
graph TB
    subgraph "Frontend Layer"
        UI1[React App :3000]
        UI2[React App :3001]
        UI3[React App :3002]
    end
    
    subgraph "API Gateway Layer"
        API[Spring Boot API :8080]
        WS[WebSocket Handler]
    end
    
    subgraph "Service Layer"
        MS[Message Service]
        US[User Service]
        CS[Connection Manager]
        DS[Message Distribution Service]
    end
    
    subgraph "Message Queue"
        KAFKA[Apache Kafka :9092]
        ZK[Zookeeper :2181]
    end
    
    subgraph "Data Layer"
        PG[(PostgreSQL :5432<br/>User Data)]
        MONGO[(MongoDB :27017<br/>Messages)]
        REDIS[(Redis :6379<br/>Sessions/Cache)]
        ES[(Elasticsearch :9200<br/>Search Index)]
    end
    
    %% Frontend connections
    UI1 -.->|WebSocket| WS
    UI2 -.->|WebSocket| WS
    UI3 -.->|WebSocket| WS
    UI1 -->|HTTP/REST| API
    UI2 -->|HTTP/REST| API
    UI3 -->|HTTP/REST| API
    
    %% API Layer
    API --> US
    API --> MS
    WS --> CS
    WS --> MS
    
    %% Service Layer
    MS -->|Publish| KAFKA
    MS -->|Store| MONGO
    DS -->|Subscribe| KAFKA
    DS --> WS
    US --> PG
    CS --> REDIS
    
    %% Dependencies
    KAFKA --> ZK
    MS -.->|Search| ES
    
    %% Styling
    classDef frontend fill:#e1f5fe
    classDef api fill:#f3e5f5
    classDef service fill:#e8f5e8
    classDef queue fill:#fff3e0
    classDef data fill:#fce4ec
    
    class UI1,UI2,UI3 frontend
    class API,WS api
    class MS,US,CS,DS service
    class KAFKA,ZK queue
    class PG,MONGO,REDIS,ES data
```

## Component Details

### Frontend Layer
- **React Applications**: Multiple instances for testing real-time capabilities
- **WebSocket Client**: Real-time bidirectional communication
- **HTTP Client**: RESTful API interactions for authentication and data retrieval

### API Gateway Layer
- **Spring Boot API**: RESTful endpoints for authentication, user management
- **WebSocket Handler**: Manages real-time connections and message routing

### Service Layer
- **Message Service**: Handles message processing, validation, and Kafka publishing
- **User Service**: User authentication, registration, and profile management
- **Connection Manager**: Tracks active user sessions and server assignments
- **Message Distribution Service**: Event-driven message broadcasting to connected clients

### Message Queue
- **Apache Kafka**: Event streaming platform for message distribution
- **Zookeeper**: Coordination service for Kafka cluster management

### Data Layer
- **PostgreSQL**: Relational database for user accounts and authentication
- **MongoDB**: Document database for chat messages and conversation history
- **Redis**: In-memory cache for sessions, real-time data, and connection tracking
- **Elasticsearch**: Search engine for message indexing and full-text search (future feature)

## Data Flow

### Message Flow
```
1. User sends message via WebSocket
2. ChatWebSocketHandler receives message
3. MessageService validates and stores in MongoDB
4. MessageService publishes to Kafka topic
5. MessageDistributionService consumes from Kafka
6. MessageDistributionService broadcasts to all connected users
7. Users receive real-time message updates
```

### Authentication Flow
```
1. User submits login credentials
2. AuthController validates against PostgreSQL
3. JWT token generated and returned
4. WebSocket connections authenticated via interceptor
5. User session stored in Redis
```

## Scalability Features

### Horizontal Scaling
- **Stateless Services**: All business logic services are stateless
- **Load Balancing**: Multiple API instances can be deployed behind a load balancer
- **Database Sharding**: MongoDB supports horizontal partitioning for message data

### Event-Driven Architecture
- **Asynchronous Processing**: Kafka enables non-blocking message processing
- **Service Decoupling**: Services communicate via events, reducing tight coupling
- **Fault Tolerance**: Message queuing provides reliability and retry mechanisms

### Caching Strategy
- **Session Caching**: Redis stores user sessions and connection metadata
- **Application Caching**: Frequently accessed data cached for performance
- **Connection Pooling**: Database connections optimized for concurrent access

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Frontend | React 18 + TypeScript | User interface and real-time messaging |
| Backend | Spring Boot 3.2 | REST API and WebSocket handling |
| Message Queue | Apache Kafka | Event streaming and message distribution |
| User Database | PostgreSQL 15 | User accounts and authentication |
| Message Store | MongoDB 6.0 | Chat messages and conversation history |
| Cache | Redis 7 | Session management and real-time data |
| Search | Elasticsearch 8.9 | Message search and indexing |
| Orchestration | Docker Compose | Local development environment |

## Performance Characteristics

### Concurrent Users
- **WebSocket Connections**: Supports 1000+ concurrent connections per instance
- **Message Throughput**: 10,000+ messages per second via Kafka
- **Response Time**: <100ms for message delivery in optimal conditions

### Storage
- **Message Retention**: Configurable retention policies in MongoDB
- **Session Storage**: Redis TTL-based session expiration
- **Search Indexing**: Real-time message indexing in Elasticsearch

## Security Considerations

### Authentication & Authorization
- **JWT Tokens**: Stateless authentication with configurable expiration
- **WebSocket Security**: Connection-level authentication via interceptors
- **CORS Configuration**: Configurable cross-origin resource sharing

### Data Protection
- **Input Validation**: Message content validation and sanitization
- **SQL Injection Prevention**: Parameterized queries and ORM usage
- **XSS Protection**: Content Security Policy headers

## Monitoring & Observability

### Metrics
- **Application Metrics**: Spring Boot Actuator with Prometheus integration
- **Custom Metrics**: Message throughput, connection counts, error rates
- **Infrastructure Metrics**: Docker container and service health

### Logging
- **Structured Logging**: JSON-formatted logs for better parsing
- **Distributed Tracing**: Request correlation across service boundaries
- **Error Tracking**: Comprehensive error logging and alerting

## Development Environment

### Local Setup
- **Docker Compose**: Single-command infrastructure setup
- **Hot Reload**: Development-time code changes without restart
- **Database Seeding**: Sample data for testing and development

### Testing Strategy
- **Unit Tests**: Service layer business logic testing
- **Integration Tests**: Database and external service interaction testing
- **E2E Testing**: Multi-user chat scenarios and WebSocket testing
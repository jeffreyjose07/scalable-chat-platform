# Scalable Chat Platform - Development Phases & Design Decisions

## 📋 Project Overview
Transform a group-only chat system into a full-featured messaging platform with private messaging, message search, and user discovery.

## 🎯 Overall Architecture Decisions

### **Core Technology Stack**
- **Backend**: Spring Boot 3.2.0 + Java 17
- **Frontend**: React 18 + TypeScript + Tailwind CSS
- **Databases**: PostgreSQL (user data) + MongoDB (messages)
- **Real-time**: WebSocket + Kafka for message distribution
- **Caching**: Redis for session management
- **Search**: MongoDB text search (simple, no Elasticsearch complexity)

### **Key Design Principles**
1. **Backward Compatibility**: Existing group chats become proper conversations
2. **Mobile-First**: Responsive design optimized for mobile WebView
3. **Scalability**: Microservices-ready architecture with clear service boundaries
4. **Security**: Conversation access validation at every layer
5. **Performance**: In-conversation search only (no global search complexity)
6. **Simplicity**: Choose simpler solutions over complex ones

---

## 🚀 Phase 0: Mobile Optimization (✅ COMPLETED)
**Duration**: 1 day | **Status**: ✅ Completed

### **Goal**: Optimize existing UI for mobile devices

### **Implemented Features**
- **Responsive sidebar**: Hidden on mobile, slide-out overlay
- **Mobile-friendly components**: Touch-optimized buttons and inputs
- **Better space utilization**: Full-width chat area on mobile
- **Improved message display**: Better text wrapping and sizing

### **Key Files Modified**
- `ChatPage.tsx` - Mobile sidebar toggle
- `MessageList.tsx` - Responsive message bubbles
- `MessageInput.tsx` - Mobile-optimized input with icon
- `ConversationList.tsx` - Touch-friendly conversation list

### **Design Decisions**
- **Hamburger menu approach**: Standard mobile UX pattern
- **Overlay vs push navigation**: Overlay chosen for better space utilization
- **Icon-only send button**: Space-saving on mobile

---

## 🔧 Phase 1: Database Schema & CI/CD (✅ COMPLETED)
**Duration**: 2 days | **Status**: ✅ Completed

### **Goal**: Establish data models and development infrastructure

### **Database Schema Design**
```sql
-- PostgreSQL Tables (User/Conversation metadata)
CREATE TABLE conversations (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(20) NOT NULL, -- 'GROUP' or 'DIRECT'
    name VARCHAR(255), -- NULL for direct messages
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE conversation_participants (
    conversation_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP DEFAULT NOW(),
    last_read_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (conversation_id, user_id)
);
```

### **Key Design Decisions**
- **Hybrid database approach**: PostgreSQL for structured data, MongoDB for messages
- **Conversation types**: GROUP vs DIRECT with different behavior
- **Participant model**: Supports leave/rejoin, read receipts
- **Direct message IDs**: `dm_${smaller_id}_${larger_id}` for consistency

### **CI/CD Pipeline**
- **GitHub Actions**: Parallel backend/frontend builds
- **Unit tests only**: H2 in-memory database, no external dependencies
- **Corporate Maven fix**: Override repository settings for CI
- **Branch protection**: Requires PR approval and passing tests

### **Migration Strategy**
- **Backward compatibility**: Existing chats become group conversations
- **Auto-enrollment**: All users added to existing groups
- **Data migration**: ConversationMigrationService handles setup

---

## 🛠️ Phase 2: Backend Services (🔄 IN PROGRESS)
**Duration**: 3 days | **Status**: 🔄 In Progress

### **Goal**: Implement core business logic and API endpoints

### **Services to Implement**

#### **1. ConversationService**
```java
@Service
public class ConversationService {
    // Create direct conversation between users
    Conversation createDirectConversation(String userId1, String userId2);
    
    // Get user's conversations with last message and unread count
    List<ConversationDto> getUserConversations(String userId);
    
    // Validate user access to conversation
    boolean hasUserAccess(String userId, String conversationId);
}
```

#### **2. UserSearchService**
```java
@Service
public class UserSearchService {
    // Search users by username, email, displayName (excluding current user)
    List<UserDto> searchUsers(String query, String currentUserId, int limit);
}
```

#### **3. MessageSearchService**
```java
@Service
public class MessageSearchService {
    // Search messages within a conversation using MongoDB text search
    SearchResultDto searchMessages(String conversationId, String query, int page, int size);
}
```

### **API Endpoints**
- `GET /api/conversations` - Get user's conversations
- `POST /api/conversations/direct` - Create direct conversation
- `GET /api/users/search` - Search users
- `GET /api/messages/search` - Search messages in conversation

### **Design Decisions**
- **MongoDB text search**: Simpler than Elasticsearch, good performance for chat
- **In-conversation search only**: No global search complexity
- **Simple access control**: User must be participant
- **Paginated search results**: Better performance for large message histories

---

## 🎨 Phase 3: Frontend Components (📋 PLANNED)
**Duration**: 3 days | **Status**: 📋 Planned

### **Goal**: Build user interface for private messaging

### **New Components**
- `ConversationTypeToggle` - Switch between Groups/Direct
- `UserSearchModal` - Find users to message
- `MessageSearchBar` - Search within conversation
- `DirectMessagesList` - List of DM conversations
- `SearchResultsList` - Display search results

### **Enhanced Components**
- `ConversationList` - Support both types with tabs
- `ChatHeader` - Add search toggle button
- `MessageList` - Highlight search results

### **User Experience Flow**
1. **Toggle between Groups/Direct** in sidebar
2. **Search for users** via search button
3. **Click user** to start/open conversation
4. **Search messages** within conversation
5. **Navigate to search results** with context

---

## 🔍 Phase 4: Message Search Integration (📋 PLANNED)
**Duration**: 2 days | **Status**: 📋 Planned

### **Goal**: Implement in-chat message search

### **MongoDB Search Implementation**
```javascript
// Create text search index
db.messages.createIndex({
  "content": "text",
  "senderUsername": "text"
})

// Search query
db.messages.find({
  $text: { $search: "query" },
  conversationId: "conv_id"
})
```

### **Search Features**
- **Real-time search**: Debounced input with instant results
- **Contextual results**: Show surrounding messages
- **Search highlighting**: Highlight matched terms
- **Performance optimized**: Pagination and query optimization

### **Design Decisions**
- **MongoDB text search**: Built-in, simple, fast enough
- **No Elasticsearch**: Avoid infrastructure complexity
- **Search scope**: Within conversation only
- **Result limit**: 20 per page for performance

---

## 🧪 Phase 5: Integration Testing (📋 PLANNED)
**Duration**: 2 days | **Status**: 📋 Planned

### **Goal**: Add comprehensive integration tests

### **Testing Strategy**
- **Unit tests**: Continue with existing approach
- **Integration tests**: Use Testcontainers for real database testing
- **End-to-end scenarios**: Test complete user workflows

### **Testcontainers Setup**
```java
@SpringBootTest
@Testcontainers
class ConversationIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");
    
    @Test
    void testCompleteConversationFlow() {
        // Test: Create conversation → Add participants → Send messages → Search
    }
}
```

### **Test Coverage Goals**
- **Unit tests**: >80% line coverage
- **Integration tests**: >70% feature coverage
- **Critical paths**: 100% coverage

---

## 📊 Design Decisions & Trade-offs

### **Database Design**
| Decision | Alternative | Rationale |
|----------|-------------|-----------|
| PostgreSQL + MongoDB | Single database | Structured data vs document storage optimization |
| Composite participant key | Separate ID | Natural key, prevents duplicates |
| Conversation types enum | Separate tables | Simpler queries, shared behavior |

### **Search Implementation**
| Decision | Alternative | Rationale |
|----------|-------------|-----------|
| MongoDB text search | Elasticsearch | Simpler setup, good enough performance |
| In-conversation search | Global search | Reduced complexity, better UX |
| Real-time search | Button-triggered | Better user experience |

### **Architecture Choices**
| Decision | Alternative | Rationale |
|----------|-------------|-----------|
| Service layer pattern | Repository direct access | Better separation of concerns |
| WebSocket validation | Trust frontend | Security-first approach |
| Feature branches | Direct main commits | Better code review process |

### **Mobile Strategy**
| Decision | Alternative | Rationale |
|----------|-------------|-----------|
| Responsive web app | Native mobile app | Faster development, shared codebase |
| WebView compatibility | Mobile-specific build | Easy Android integration |
| Hamburger menu | Bottom navigation | Standard mobile UX pattern |

---

## 🔄 Development Workflow

### **Branch Strategy**
```
main                    # Protected, production-ready
├── feature/phase2-*    # Short-lived feature branches
├── bugfix/fix-*        # Bug fixes
└── hotfix/urgent-*     # Critical fixes
```

### **Commit Standards**
- **feat**: New features
- **fix**: Bug fixes  
- **refactor**: Code improvements
- **test**: Test additions
- **docs**: Documentation updates

### **Code Review Process**
1. **Feature branch creation**
2. **Implementation with unit tests**
3. **Pull request creation**
4. **CI/CD validation**
5. **Code review approval**
6. **Merge to main**

---

## 🚀 Deployment Strategy

### **Environment Progression**
1. **Development**: Local with Docker Compose
2. **Staging**: Automated deployment from main
3. **Production**: Manual deployment with approval

### **Feature Flags**
- `features.direct-messaging`: Enable/disable private messaging
- `features.message-search`: Enable/disable search functionality
- `features.user-discovery`: Enable/disable user search

### **Rollback Strategy**
- **Database migrations**: Backward compatible
- **API versioning**: Maintain compatibility
- **Feature flags**: Instant disable capability

---

## 📈 Success Metrics

### **Performance Targets**
- **Message search**: <500ms response time
- **Conversation loading**: <200ms
- **WebSocket connection**: <100ms
- **Mobile responsiveness**: <16ms frame time

### **User Experience Goals**
- **Mobile usability**: Touch-friendly interfaces
- **Search accuracy**: Relevant results first
- **Real-time messaging**: <100ms message delivery
- **Offline resilience**: Graceful degradation

### **Technical Metrics**
- **Test coverage**: >80% overall
- **Build time**: <5 minutes
- **Deployment frequency**: Multiple per day
- **Mean time to recovery**: <30 minutes

---

## 🔮 Future Enhancements

### **Phase 6+: Advanced Features**
- **File sharing**: Image and document support
- **Voice messages**: Audio recording/playback
- **Message reactions**: Emoji reactions
- **Read receipts**: Message seen indicators
- **Typing indicators**: Real-time typing status
- **Push notifications**: Mobile notification system

### **Scaling Considerations**
- **Message archiving**: Move old messages to cold storage
- **Sharding strategy**: Distribute conversations across databases
- **CDN integration**: Static asset delivery
- **Microservices**: Break into smaller services

---

This document serves as the single source of truth for the project's direction, decisions, and implementation strategy. All phases are designed to be incremental, testable, and maintain backward compatibility.
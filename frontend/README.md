# Chat Platform Frontend

A modern React frontend for a scalable chat platform with real-time messaging, group management, and responsive design.

## 🚀 Features

### Core Functionality
- **Real-time messaging** via WebSocket with automatic reconnection
- **Responsive design** with mobile-first approach
- **Direct messaging** between users
- **Group conversations** with advanced management
- **Conversation deletion** with role-based permissions
- **Message search** with highlighting and pagination
- **Unread message tracking** with visual indicators
- **User authentication** with JWT tokens
- **Network access support** for multi-device testing

### Advanced Group Management
- **Group creation modal** with customizable settings
- **Role-based UI** showing permissions (OWNER/ADMIN/MEMBER)
- **Participant management** interface
- **Group settings panel** for metadata updates
- **Group deletion** with owner-only permissions
- **Permission-aware controls** based on user role

### User Experience
- **Modern Material Design** with Tailwind CSS
- **Dark/Light theme** support
- **Emoji support** in messages
- **Typing indicators** and online status
- **Message timestamps** and read receipts
- **Responsive layout** for all device sizes

## 📋 Requirements

- Node.js 16+
- npm 8+
- Modern web browser with WebSocket support

## 🛠️ Quick Start

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Development Server
```bash
# Local development
npm start

# Network access (for testing from other devices)
npm run start:network
```

### 3. Build for Production
```bash
npm run build
```

## 🏗️ Project Structure

```
frontend/src/
├── components/
│   ├── auth/
│   │   ├── LoginForm.tsx         # Authentication form
│   │   └── AuthGuard.tsx         # Route protection
│   ├── chat/
│   │   ├── ChatWindow.tsx        # Main chat interface
│   │   ├── MessageList.tsx       # Message display
│   │   ├── MessageInput.tsx      # Message composition
│   │   └── MessageSearch.tsx     # Search functionality
│   ├── conversations/
│   │   ├── ConversationList.tsx  # Conversation sidebar
│   │   ├── ConversationItem.tsx  # Individual conversation
│   │   └── CreateGroupModal.tsx  # Group creation UI
│   ├── groups/
│   │   ├── GroupSettingsModal.tsx # Group settings UI
│   │   ├── ParticipantList.tsx   # Participant management
│   │   └── RoleIndicator.tsx     # Role display component
│   ├── users/
│   │   ├── UserList.tsx          # User discovery
│   │   ├── UserItem.tsx          # User display
│   │   └── OnlineStatus.tsx      # Online indicator
│   └── common/
│       ├── Layout.tsx            # Main layout component
│       ├── Header.tsx            # App header
│       ├── LoadingSpinner.tsx    # Loading indicator
│       └── ErrorBoundary.tsx     # Error handling
├── hooks/
│   ├── useAuth.tsx               # Authentication hook
│   ├── useWebSocket.tsx          # WebSocket connection
│   ├── useConversations.tsx      # Conversation management
│   ├── useMessages.tsx           # Message handling
│   ├── useUnreadMessages.tsx     # Unread tracking
│   └── useUsers.tsx              # User management
├── services/
│   ├── api.ts                    # API client
│   ├── websocket.ts              # WebSocket service
│   ├── auth.ts                   # Authentication service
│   └── storage.ts                # Local storage utilities
├── types/
│   ├── auth.ts                   # Authentication types
│   ├── conversation.ts           # Conversation types
│   ├── message.ts                # Message types
│   ├── user.ts                   # User types
│   └── websocket.ts              # WebSocket types
├── utils/
│   ├── dateUtils.ts              # Date formatting
│   ├── networkUtils.ts           # Network utilities
│   ├── messageUtils.ts           # Message processing
│   └── constants.ts              # Application constants
├── pages/
│   ├── LoginPage.tsx             # Login page
│   ├── ChatPage.tsx              # Main chat page
│   └── NotFoundPage.tsx          # 404 page
└── styles/
    ├── globals.css               # Global styles
    └── components.css            # Component styles
```

## 🎨 Design System

### Color Palette
```css
/* Primary Colors */
--primary-blue: #3B82F6;
--primary-blue-dark: #2563EB;
--primary-blue-light: #93C5FD;

/* Secondary Colors */
--secondary-gray: #6B7280;
--secondary-gray-light: #F3F4F6;
--secondary-gray-dark: #374151;

/* Status Colors */
--success-green: #10B981;
--warning-yellow: #F59E0B;
--error-red: #EF4444;
--info-blue: #3B82F6;
```

### Typography
```css
/* Font Families */
--font-primary: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
--font-mono: 'Fira Code', 'Monaco', monospace;

/* Font Sizes */
--text-xs: 0.75rem;
--text-sm: 0.875rem;
--text-base: 1rem;
--text-lg: 1.125rem;
--text-xl: 1.25rem;
--text-2xl: 1.5rem;
```

## 🔧 Configuration

### Environment Variables
```bash
# API Configuration
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080

# Feature Flags
REACT_APP_ENABLE_DARK_MODE=true
REACT_APP_ENABLE_NOTIFICATIONS=true
REACT_APP_ENABLE_SEARCH=true

# Development
REACT_APP_DEBUG=true
REACT_APP_LOG_LEVEL=debug
```

### Build Configuration
```json
{
  "scripts": {
    "start": "react-scripts start",
    "start:network": "REACT_APP_API_BASE_URL=http://$(hostname -I | cut -d' ' -f1):8080 react-scripts start --host 0.0.0.0",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "test:coverage": "react-scripts test --coverage --watchAll=false",
    "eject": "react-scripts eject"
  }
}
```

## 🧪 Testing

### Unit Tests
```bash
# Run all tests
npm test

# Run tests with coverage
npm run test:coverage

# Run specific test file
npm test -- --testNamePattern="ConversationList"

# Run tests in watch mode
npm test -- --watch
```

### Testing Strategy
- **Component Testing**: React Testing Library for UI components
- **Hook Testing**: Custom hooks with React Hooks Testing Library
- **Integration Testing**: End-to-end user flows
- **Mock Services**: API and WebSocket mocking for isolated testing

### Example Test
```typescript
// ConversationList.test.tsx
import { render, screen } from '@testing-library/react';
import { ConversationList } from './ConversationList';

describe('ConversationList', () => {
  it('renders conversation items', () => {
    const mockConversations = [
      { id: '1', name: 'Test Group', type: 'GROUP' },
      { id: '2', name: 'Direct Message', type: 'DIRECT' }
    ];
    
    render(<ConversationList conversations={mockConversations} />);
    
    expect(screen.getByText('Test Group')).toBeInTheDocument();
    expect(screen.getByText('Direct Message')).toBeInTheDocument();
  });
});
```

## 📱 Responsive Design

### Breakpoints
```css
/* Mobile First Approach */
@media (min-width: 640px) { /* sm */ }
@media (min-width: 768px) { /* md */ }
@media (min-width: 1024px) { /* lg */ }
@media (min-width: 1280px) { /* xl */ }
@media (min-width: 1536px) { /* 2xl */ }
```

### Layout Strategy
- **Mobile**: Single-column layout with tab navigation
- **Tablet**: Two-column layout (sidebar + main content)
- **Desktop**: Three-column layout (sidebar + chat + details)

## 🔌 WebSocket Integration

### Connection Management
```typescript
const useWebSocket = () => {
  const [socket, setSocket] = useState<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  
  useEffect(() => {
    const wsUrl = process.env.REACT_APP_WS_URL || 'ws://localhost:8080';
    const ws = new WebSocket(`${wsUrl}/ws/chat`);
    
    ws.onopen = () => setIsConnected(true);
    ws.onclose = () => setIsConnected(false);
    ws.onerror = (error) => console.error('WebSocket error:', error);
    
    setSocket(ws);
    
    return () => ws.close();
  }, []);
  
  return { socket, isConnected };
};
```

### Message Handling
```typescript
const useMessages = (conversationId: string) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const { socket } = useWebSocket();
  
  useEffect(() => {
    if (!socket) return;
    
    const handleMessage = (event: MessageEvent) => {
      const data = JSON.parse(event.data);
      if (data.type === 'MESSAGE' && data.conversationId === conversationId) {
        setMessages(prev => [...prev, data.message]);
      }
    };
    
    socket.addEventListener('message', handleMessage);
    
    return () => socket.removeEventListener('message', handleMessage);
  }, [socket, conversationId]);
  
  return { messages };
};
```

## 🎯 Performance Optimization

### Code Splitting
```typescript
// Lazy loading for pages
const ChatPage = lazy(() => import('./pages/ChatPage'));
const GroupSettingsModal = lazy(() => import('./components/groups/GroupSettingsModal'));

// Route-based splitting
const App = () => (
  <Suspense fallback={<LoadingSpinner />}>
    <Routes>
      <Route path="/chat" element={<ChatPage />} />
    </Routes>
  </Suspense>
);
```

### Memoization
```typescript
// Memoized components
const MessageItem = memo(({ message }: { message: Message }) => (
  <div className="message-item">
    {message.content}
  </div>
));

// Memoized computations
const sortedConversations = useMemo(() => 
  conversations.sort((a, b) => 
    new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  ), [conversations]
);
```

### Virtual Scrolling
```typescript
// Large message lists
const MessageList = ({ messages }: { messages: Message[] }) => {
  const [visibleRange, setVisibleRange] = useState({ start: 0, end: 50 });
  
  const visibleMessages = useMemo(() => 
    messages.slice(visibleRange.start, visibleRange.end),
    [messages, visibleRange]
  );
  
  return (
    <div className="message-list">
      {visibleMessages.map(message => (
        <MessageItem key={message.id} message={message} />
      ))}
    </div>
  );
};
```

## 🔒 Security

### Authentication
```typescript
// JWT token management
const useAuth = () => {
  const [token, setToken] = useState<string | null>(
    localStorage.getItem('authToken')
  );
  
  const login = async (credentials: LoginCredentials) => {
    const response = await api.post('/auth/login', credentials);
    const { token } = response.data;
    
    localStorage.setItem('authToken', token);
    setToken(token);
  };
  
  const logout = () => {
    localStorage.removeItem('authToken');
    setToken(null);
  };
  
  return { token, login, logout };
};
```

### Input Sanitization
```typescript
// Message sanitization
const sanitizeMessage = (content: string): string => {
  return content
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;');
};
```

## 🚀 Production Deployment

### Build Optimization
```bash
# Production build
npm run build

# Analyze bundle size
npm install -g webpack-bundle-analyzer
npx webpack-bundle-analyzer build/static/js/*.js
```

### Deployment Configuration
```dockerfile
# Multi-stage Dockerfile
FROM node:16-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Environment Configuration
```bash
# Production environment variables
REACT_APP_API_BASE_URL=https://api.yourhost.com
REACT_APP_WS_URL=wss://api.yourhost.com
REACT_APP_ENABLE_DEBUG=false
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Use TypeScript for type safety
- Follow ESLint and Prettier configuration
- Use meaningful component and variable names
- Write comprehensive tests for new features
- Follow React best practices and hooks patterns

## 📝 License

This project is for educational and demonstration purposes.
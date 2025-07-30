import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { useAuth } from './useAuth';
import { ChatMessage, MessageStatusUpdate, WebSocketMessage } from '../types/chat';
import { messageService } from '../services/messageService';
import toast from 'react-hot-toast';
import { getWebSocketUrl } from '../utils/networkUtils';

interface WebSocketContextType {
  socket: WebSocket | null;
  isConnected: boolean;
  sendMessage: (message: Omit<ChatMessage, 'id' | 'timestamp'>) => void;
  sendMessageStatusUpdate: (statusUpdate: MessageStatusUpdate) => void;
  messages: ChatMessage[];
  loadConversationMessages: (conversationId: string, forceReload?: boolean) => Promise<void>;
  isLoadingMessages: boolean;
  isReconnecting: boolean;
  reconnectAttempts: number;
  clearMessagesCache: () => void;
}

const WebSocketContext = createContext<WebSocketContextType | null>(null);

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within WebSocketProvider');
  }
  return context;
};

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [socket, setSocket] = useState<WebSocket | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [messagesLoaded, setMessagesLoaded] = useState(false);
  const [isIntentionalDisconnect, setIsIntentionalDisconnect] = useState(false);
  const [hasShownConnectedToast, setHasShownConnectedToast] = useState(false);
  const [lastSentMessage, setLastSentMessage] = useState<{content: string, timestamp: number} | null>(null);
  const [wasEverConnected, setWasEverConnected] = useState(false);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [loadedConversations, setLoadedConversations] = useState<Set<string>>(new Set());
  const [reconnectAttempts, setReconnectAttempts] = useState(0);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const { user, token } = useAuth();
  
  // Reconnection configuration - optimized for faster reconnection
  const MAX_RECONNECT_ATTEMPTS = 8;
  const BASE_RECONNECT_DELAY = 300; // 300ms instead of 1 second
  
  // Reconnection with exponential backoff
  const attemptReconnect = useCallback(() => {
    if (isIntentionalDisconnect || !user || !token) {
      return;
    }
    
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      console.log('Max reconnection attempts reached');
      setIsReconnecting(false);
      toast.error('Unable to reconnect to chat server. Please refresh the page.');
      return;
    }
    
    const delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts);
    console.log(`Attempting to reconnect in ${delay}ms (attempt ${reconnectAttempts + 1}/${MAX_RECONNECT_ATTEMPTS})`);
    
    setIsReconnecting(true);
    setReconnectAttempts(prev => prev + 1);
    
    setTimeout(() => {
      if (!isIntentionalDisconnect && !isConnected) {
        console.log('Executing reconnection attempt');
        // Force recreation of WebSocket connection
        setSocket(null);
      }
    }, delay);
  }, [reconnectAttempts, isIntentionalDisconnect, user, token, isConnected]);

  // Load conversation messages function
  const loadConversationMessages = async (conversationId: string, forceReload = false) => {
    if (!user || !token) {
      console.log('Skipping conversation load: missing user or token', { user: !!user, token: !!token });
      return;
    }
    
    if (!forceReload && loadedConversations.has(conversationId)) {
      console.log('Conversation already loaded, skipping:', conversationId);
      return;
    }
    
    setIsLoadingMessages(true);
    try {
      console.log(`🔄 Loading messages for conversation: ${conversationId}`);
      const conversationMessages = await messageService.fetchConversationMessages(conversationId, token);
      console.log(`✅ Fetched ${conversationMessages.length} messages for conversation: ${conversationId}`);
      
      setMessages(prev => {
        console.log(`📝 Before update: ${prev.length} total messages, ${prev.filter(m => m.conversationId === conversationId).length} from this conversation`);
        
        // Remove any existing messages from this conversation to avoid duplicates
        const filteredPrev = prev.filter(msg => msg.conversationId !== conversationId);
        console.log(`🗑️ After filtering out conversation ${conversationId}: ${filteredPrev.length} messages remaining`);
        
        // Add the new conversation messages
        const combined = [...filteredPrev, ...conversationMessages];
        console.log(`➕ After adding new messages: ${combined.length} total messages`);
        
        // Sort by timestamp to maintain chronological order
        const sorted = combined.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
        console.log(`🔄 After sorting: ${sorted.length} messages, conversation ${conversationId} has ${sorted.filter(m => m.conversationId === conversationId).length} messages`);
        
        return sorted;
      });
      
      setLoadedConversations(prev => {
        const newSet = new Set(prev);
        newSet.add(conversationId);
        console.log(`📚 Marked conversation ${conversationId} as loaded. Total loaded: ${newSet.size}`);
        return newSet;
      });
    } catch (error) {
      console.error(`❌ Error loading messages for conversation ${conversationId}:`, error);
    } finally {
      setIsLoadingMessages(false);
    }
  };

  // Load recent messages only on initial load (with optional caching)
  useEffect(() => {
    if (!user || !token || messagesLoaded) return;
    
    const loadInitialMessages = async () => {
      try {
        console.log('🔄 Loading initial messages...');
        
        // Disable cache temporarily to debug issues - try cache first for instant load
        const USE_CACHE = false; // Set to false to disable cache temporarily
        
        if (USE_CACHE) {
          try {
            const cached = sessionStorage.getItem('recent_messages');
            if (cached) {
              const parsedCache = JSON.parse(cached);
              // Use cache if less than 10 seconds old (reduced from 30s)
              if (Date.now() - parsedCache.timestamp < 10000) {
                console.log('📦 Using cached recent messages for instant load');
                setMessages(parsedCache.data);
                setMessagesLoaded(true);
                return; // Skip API call if cache is fresh
              } else {
                console.log('📦 Cache expired, loading fresh messages');
              }
            }
          } catch (error) {
            console.warn('Failed to load cached messages:', error);
          }
        } else {
          console.log('📦 Cache disabled, loading fresh messages');
        }
        
        const recentMessages = await messageService.fetchRecentMessages(token);
        console.log(`✅ Loaded ${recentMessages.length} recent messages`);
        setMessages(recentMessages);
        setMessagesLoaded(true);
        
        // Cache for next time (if enabled)
        if (USE_CACHE) {
          try {
            sessionStorage.setItem('recent_messages', JSON.stringify({
              data: recentMessages,
              timestamp: Date.now()
            }));
          } catch (error) {
            console.warn('Failed to cache messages:', error);
          }
        }
        
        console.log('Initial recent messages loaded on component mount');
      } catch (error) {
        console.error('Error loading initial messages:', error);
      }
    };
    
    loadInitialMessages();
  }, [user, token, messagesLoaded]);

  useEffect(() => {
    if (!user || !token) {
      console.log('WebSocket not connecting: missing user or token', { user: !!user, token: !!token });
      return;
    }

    // Prevent creating multiple connections
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      console.log('WebSocket already connected/connecting, skipping new connection');
      return;
    }

    // Clean up any existing connection
    if (socket && (socket.readyState === WebSocket.CLOSED || socket.readyState === WebSocket.CLOSING)) {
      console.log('Cleaning up closed WebSocket connection');
      setSocket(null);
    }

    const wsUrl = getWebSocketUrl();
    const fullWsUrl = `${wsUrl}/ws/chat?token=${token}`;
    
    console.log('Attempting WebSocket connection to:', fullWsUrl);
    console.log('User info:', { id: user.id, username: user.username });
    console.log('Token (first 20 chars):', token.substring(0, 20) + '...');
    
    const newSocket = new WebSocket(fullWsUrl);

    newSocket.onopen = async () => {
      console.log('WebSocket connected successfully');
      setIsConnected(true);
      setIsIntentionalDisconnect(false);
      setWasEverConnected(true);
      
      // Reset reconnection state on successful connection
      setReconnectAttempts(0);
      setIsReconnecting(false);
      
      // Show appropriate toast message
      if (reconnectAttempts > 0) {
        toast.success('Reconnected to chat server');
      } else if (!hasShownConnectedToast) {
        toast.success('Connected to chat server');
        setHasShownConnectedToast(true);
      }
      
      // Skip loading messages here - they're already loaded by the initial useEffect
      // This eliminates duplicate loading and speeds up connection
      console.log('WebSocket connected - messages already loaded, skipping duplicate load');
    };

    newSocket.onclose = (event) => {
      console.log('WebSocket closed:', { code: event.code, reason: event.reason, wasClean: event.wasClean });
      setIsConnected(false);
      
      // Reset toast flag to allow showing connected message again after disconnect
      if (!isIntentionalDisconnect) {
        setHasShownConnectedToast(false);
      }
      
      // Only show disconnect toast if:
      // 1. It's not an intentional disconnect (like page refresh)
      // 2. We were previously connected (not initial connection failures)
      // 3. It's not a normal close code (1000 = normal, 1001 = going away)
      if (!isIntentionalDisconnect && wasEverConnected && event.code !== 1000 && event.code !== 1001) {
        if (reconnectAttempts === 0) {
          toast.error('Disconnected from chat server. Attempting to reconnect...');
        }
        attemptReconnect();
      }
    };

    newSocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        
        if (data.type === 'ack') {
          // Handle acknowledgment
          console.log('Message acknowledged:', data.messageId);
        } else if (data.type === 'error') {
          toast.error(data.message);
        } else if (data.type === 'ping') {
          // Handle ping message - send pong response
          console.log('Received ping from server, sending pong');
          try {
            const pongResponse = JSON.stringify({
              type: 'pong',
              timestamp: data.timestamp
            });
            newSocket.send(pongResponse);
          } catch (error) {
            console.error('Error sending pong response:', error);
          }
        } else if (data.type === 'MESSAGE_DELIVERED' || data.type === 'MESSAGE_READ') {
          // Handle message status updates
          console.log('Received message status update:', data);
          const statusUpdate: MessageStatusUpdate = data.data;
          
          // Update the corresponding message's status in our state
          setMessages(prev => prev.map(msg => {
            if (msg.id === statusUpdate.messageId) {
              const updatedMessage = { ...msg };
              
              if (data.type === 'MESSAGE_DELIVERED') {
                updatedMessage.deliveredTo = {
                  ...updatedMessage.deliveredTo,
                  [statusUpdate.userId]: statusUpdate.timestamp
                };
              } else if (data.type === 'MESSAGE_READ') {
                // Ensure it's also marked as delivered
                updatedMessage.deliveredTo = {
                  ...updatedMessage.deliveredTo,
                  [statusUpdate.userId]: statusUpdate.timestamp
                };
                updatedMessage.readBy = {
                  ...updatedMessage.readBy,
                  [statusUpdate.userId]: statusUpdate.timestamp
                };
              }
              
              return updatedMessage;
            }
            return msg;
          }));
        } else {
          // Handle regular chat message
          const message: ChatMessage = data;
          console.log('Received message:', {
            id: message.id,
            senderId: message.senderId,
            senderUsername: message.senderUsername,
            content: message.content?.substring(0, 50) + '...',
            timestamp: message.timestamp
          });
          
          // Validate message has required fields
          if (!message.senderUsername && message.senderId) {
            console.warn('Message missing senderUsername, using senderId as fallback');
          }
          
          // Add new message (real-time)
          setMessages(prev => {
            const exists = prev.some(msg => msg.id === message.id);
            if (exists) {
              console.log('Duplicate message detected, skipping:', message.id);
              return prev;
            }
            // Insert message in correct chronological position
            const newMessages = [...prev, message];
            return newMessages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
          });
        }
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    newSocket.onerror = (error) => {
      console.error('WebSocket error:', error);
      console.error('WebSocket readyState:', newSocket.readyState);
      console.error('WebSocket URL:', fullWsUrl);
      
      // Only show error toast for genuine connection failures
      // Skip errors during page refresh or component unmount
      if (!isIntentionalDisconnect && wasEverConnected) {
        console.warn('WebSocket error after successful connection - may need to reconnect');
      }
    };

    setSocket(newSocket);

    return () => {
      setIsIntentionalDisconnect(true);
      if (newSocket.readyState === WebSocket.OPEN || newSocket.readyState === WebSocket.CONNECTING) {
        newSocket.close();
      }
    };
  }, [user?.id, token]);

  const sendMessage = (message: Omit<ChatMessage, 'id' | 'timestamp'>) => {
    if (socket && isConnected) {
      const messageId = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      const messageContent = message.content.trim();
      const now = Date.now();
      
      // Prevent sending duplicate messages within 1 second (rapid clicking prevention)
      if (lastSentMessage && 
          lastSentMessage.content === messageContent && 
          (now - lastSentMessage.timestamp) < 1000) {
        console.log('Preventing rapid duplicate message send');
        return;
      }
      
      const fullMessage: ChatMessage = {
        ...message,
        id: messageId,
        content: messageContent,
        timestamp: new Date().toISOString(),
      };
      
      setLastSentMessage({ content: messageContent, timestamp: now });
      console.log('Sending message:', { id: messageId, content: messageContent.substring(0, 50) });
      socket.send(JSON.stringify(fullMessage));
      // Don't add to messages here - wait for it to come back through WebSocket
    }
  };

  const sendMessageStatusUpdate = (statusUpdate: MessageStatusUpdate) => {
    if (socket && isConnected) {
      const wsMessage: WebSocketMessage = {
        type: statusUpdate.statusType === 'DELIVERED' ? 'MESSAGE_DELIVERED' : 'MESSAGE_READ',
        data: {
          ...statusUpdate,
          timestamp: new Date().toISOString()
        }
      };
      
      console.log('Sending message status update:', wsMessage);
      socket.send(JSON.stringify(wsMessage));
    }
  };

  const clearMessagesCache = () => {
    console.log('🧹 Clearing messages cache and reloading');
    setMessages([]);
    setMessagesLoaded(false);
    setLoadedConversations(new Set());
    try {
      sessionStorage.removeItem('recent_messages');
    } catch (error) {
      console.warn('Failed to clear message cache:', error);
    }
  };

  return (
    <WebSocketContext.Provider value={{ 
      socket, 
      isConnected, 
      sendMessage, 
      sendMessageStatusUpdate,
      messages, 
      loadConversationMessages, 
      isLoadingMessages,
      isReconnecting,
      reconnectAttempts,
      clearMessagesCache
    }}>
      {children}
    </WebSocketContext.Provider>
  );
};
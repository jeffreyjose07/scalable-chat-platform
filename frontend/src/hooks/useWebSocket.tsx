import React, { createContext, useContext, useEffect, useState } from 'react';
import { useAuth } from './useAuth';
import { ChatMessage } from '../types/chat';
import { messageService } from '../services/messageService';
import toast from 'react-hot-toast';
import { getWebSocketUrl } from '../utils/networkUtils';

interface WebSocketContextType {
  socket: WebSocket | null;
  isConnected: boolean;
  sendMessage: (message: Omit<ChatMessage, 'id' | 'timestamp'>) => void;
  messages: ChatMessage[];
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
  const [lastSentMessageId, setLastSentMessageId] = useState<string | null>(null);
  const [wasEverConnected, setWasEverConnected] = useState(false);
  const { user, token } = useAuth();

  // Load messages immediately when user and token are available
  useEffect(() => {
    if (!user || !token || messagesLoaded) return;
    
    const loadInitialMessages = async () => {
      try {
        const recentMessages = await messageService.fetchRecentMessages(token);
        setMessages(recentMessages);
        setMessagesLoaded(true);
        console.log('Historical messages loaded on component mount');
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
    if (socket && socket.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected, skipping new connection');
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
      
      // Only show connected toast once per session or after a disconnect
      if (!hasShownConnectedToast) {
        toast.success('Connected to chat server');
        setHasShownConnectedToast(true);
      }
      
      // Load recent messages when connected (if not already loaded)
      if (!messagesLoaded) {
        try {
          const recentMessages = await messageService.fetchRecentMessages(token);
          setMessages(recentMessages);
          setMessagesLoaded(true);
          console.log('Historical messages loaded on WebSocket connection');
        } catch (error) {
          console.error('Error loading recent messages:', error);
        }
      }
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
        toast.error('Disconnected from chat server');
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
          
          // Prevent duplicate messages
          setMessages(prev => {
            const exists = prev.some(msg => msg.id === message.id);
            if (exists) {
              console.log('Duplicate message detected, skipping:', message.id);
              return prev;
            }
            return [...prev, message];
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
      
      // Create unique identifier for content-based deduplication
      const contentId = `${messageContent}-${message.conversationId}-${message.senderId}`;
      
      // Prevent sending duplicate messages based on content
      if (lastSentMessageId === contentId) {
        console.log('Preventing duplicate message send');
        return;
      }
      
      const fullMessage: ChatMessage = {
        ...message,
        id: messageId,
        content: messageContent,
        timestamp: new Date().toISOString(),
      };
      
      setLastSentMessageId(contentId);
      console.log('Sending message:', { id: messageId, content: messageContent.substring(0, 50) });
      socket.send(JSON.stringify(fullMessage));
      // Don't add to messages here - wait for it to come back through WebSocket
    }
  };

  return (
    <WebSocketContext.Provider value={{ socket, isConnected, sendMessage, messages }}>
      {children}
    </WebSocketContext.Provider>
  );
};
import React, { createContext, useContext, useEffect, useState } from 'react';
import { useAuth } from './useAuth';
import { ChatMessage } from '../types/chat';
import { messageService } from '../services/messageService';
import toast from 'react-hot-toast';

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

    const wsUrl = process.env.REACT_APP_WS_URL || 'ws://localhost:8080';
    const fullWsUrl = `${wsUrl}/ws/chat?token=${token}`;
    
    console.log('Attempting WebSocket connection to:', fullWsUrl);
    console.log('User info:', { id: user.id, username: user.username });
    console.log('Token (first 20 chars):', token.substring(0, 20) + '...');
    
    const newSocket = new WebSocket(fullWsUrl);

    newSocket.onopen = async () => {
      console.log('WebSocket connected successfully');
      setIsConnected(true);
      toast.success('Connected to chat server');
      
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
      toast.error('Disconnected from chat server');
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
          
          setMessages(prev => [...prev, message]);
        }
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    newSocket.onerror = (error) => {
      console.error('WebSocket error:', error);
      console.error('WebSocket readyState:', newSocket.readyState);
      console.error('WebSocket URL:', fullWsUrl);
      toast.error('Connection error');
    };

    setSocket(newSocket);

    return () => {
      newSocket.close();
    };
  }, [user, token]);

  const sendMessage = (message: Omit<ChatMessage, 'id' | 'timestamp'>) => {
    if (socket && isConnected) {
      const fullMessage: ChatMessage = {
        ...message,
        id: Date.now().toString(),
        timestamp: new Date().toISOString(),
      };
      
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
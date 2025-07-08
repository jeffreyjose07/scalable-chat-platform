import React, { createContext, useContext, useEffect, useState } from 'react';
import { useAuth } from './useAuth';
import { ChatMessage } from '../types/chat';
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
  const { user, token } = useAuth();

  useEffect(() => {
    if (!user || !token) return;

    const wsUrl = process.env.REACT_APP_WS_URL || 'ws://localhost:8080';
    const newSocket = new WebSocket(`${wsUrl}/ws/chat?token=${token}`);

    newSocket.onopen = () => {
      setIsConnected(true);
      toast.success('Connected to chat server');
    };

    newSocket.onclose = () => {
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
          setMessages(prev => [...prev, message]);
        }
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    newSocket.onerror = (error) => {
      console.error('WebSocket error:', error);
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
      setMessages(prev => [...prev, fullMessage]);
    }
  };

  return (
    <WebSocketContext.Provider value={{ socket, isConnected, sendMessage, messages }}>
      {children}
    </WebSocketContext.Provider>
  );
};
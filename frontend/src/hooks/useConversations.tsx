import { useState, useEffect, useCallback } from 'react';
import { Conversation } from '../types/chat';
import { api } from '../services/api';

interface UseConversationsReturn {
  conversations: Conversation[];
  isLoading: boolean;
  error: string | null;
  loadConversations: () => Promise<void>;
  addConversation: (conversation: Conversation) => void;
  updateConversation: (conversationId: string, updates: Partial<Conversation>) => void;
  removeConversation: (conversationId: string) => void;
}

export const useConversations = (): UseConversationsReturn => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadConversations = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      console.log('🔄 Loading conversations...');
      const userConversations = await api.conversation.getUserConversations();
      console.log('📋 Loaded conversations:', userConversations);
      console.log('🔍 Group conversations:', userConversations.filter(c => c.type === 'GROUP'));
      console.log('🔍 Direct conversations:', userConversations.filter(c => c.type === 'DIRECT'));
      setConversations(userConversations);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load conversations';
      setError(errorMessage);
      console.error('❌ Failed to load conversations:', err);
      
      // Security fix: Never show fallback conversations to prevent unauthorized access
      // User will see empty list if API fails, which is more secure
      setConversations([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const addConversation = useCallback((conversation: Conversation) => {
    setConversations(prev => {
      const exists = prev.some(conv => conv.id === conversation.id);
      if (exists) {
        return prev;
      }
      return [...prev, conversation];
    });
  }, []);

  const updateConversation = useCallback((conversationId: string, updates: Partial<Conversation>) => {
    setConversations(prev => 
      prev.map(conv => 
        conv.id === conversationId 
          ? { ...conv, ...updates }
          : conv
      )
    );
  }, []);

  const removeConversation = useCallback((conversationId: string) => {
    setConversations(prev => prev.filter(conv => conv.id !== conversationId));
  }, []);

  // Load conversations on mount
  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  return {
    conversations,
    isLoading,
    error,
    loadConversations,
    addConversation,
    updateConversation,
    removeConversation
  };
};
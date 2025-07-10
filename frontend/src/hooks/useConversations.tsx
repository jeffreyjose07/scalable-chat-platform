import { useState, useEffect, useCallback } from 'react';
import { Conversation } from '../components/ConversationList';
import { api } from '../services/api';

interface UseConversationsReturn {
  conversations: Conversation[];
  isLoading: boolean;
  error: string | null;
  loadConversations: () => Promise<void>;
  addConversation: (conversation: Conversation) => void;
  updateConversation: (conversationId: string, updates: Partial<Conversation>) => void;
}

export const useConversations = (): UseConversationsReturn => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadConversations = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const userConversations = await api.conversation.getUserConversations();
      setConversations(userConversations);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load conversations';
      setError(errorMessage);
      console.error('Failed to load conversations:', err);
      
      // Fallback to default conversations for development
      setConversations([
        { id: 'general', name: 'General Chat', type: 'GROUP' },
        { id: 'random', name: 'Random', type: 'GROUP' },
        { id: 'tech', name: 'Tech Talk', type: 'GROUP' },
      ]);
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
    updateConversation
  };
};
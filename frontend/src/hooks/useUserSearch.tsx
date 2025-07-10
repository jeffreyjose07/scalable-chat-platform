import { useState, useCallback } from 'react';
import { User } from '../components/UserSearchModal';
import { Conversation } from '../components/ConversationList';
import { api } from '../services/api';

interface UseUserSearchReturn {
  isUserSearchModalOpen: boolean;
  openUserSearchModal: () => void;
  closeUserSearchModal: () => void;
  searchUsers: (query: string) => Promise<User[]>;
  getUserSuggestions: () => Promise<User[]>;
  createDirectConversation: (user: User, onSuccess: (conversation: Conversation) => void) => Promise<void>;
}

export const useUserSearch = (): UseUserSearchReturn => {
  const [isUserSearchModalOpen, setIsUserSearchModalOpen] = useState(false);

  const openUserSearchModal = useCallback(() => {
    setIsUserSearchModalOpen(true);
  }, []);

  const closeUserSearchModal = useCallback(() => {
    setIsUserSearchModalOpen(false);
  }, []);

  const searchUsers = useCallback(async (query: string): Promise<User[]> => {
    try {
      return await api.user.searchUsers(query);
    } catch (error) {
      console.error('Failed to search users:', error);
      throw error;
    }
  }, []);

  const getUserSuggestions = useCallback(async (): Promise<User[]> => {
    try {
      return await api.user.getUserSuggestions();
    } catch (error) {
      console.error('Failed to get user suggestions:', error);
      throw error;
    }
  }, []);

  const createDirectConversation = useCallback(async (
    selectedUser: User, 
    onSuccess: (conversation: Conversation) => void
  ) => {
    try {
      const conversation = await api.conversation.createDirectConversation(selectedUser.id);
      onSuccess(conversation);
      closeUserSearchModal();
    } catch (error) {
      console.error('Failed to create direct conversation:', error);
      throw error;
    }
  }, []);

  return {
    isUserSearchModalOpen,
    openUserSearchModal,
    closeUserSearchModal,
    searchUsers,
    getUserSuggestions,
    createDirectConversation
  };
};
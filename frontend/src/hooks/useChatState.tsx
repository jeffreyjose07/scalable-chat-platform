import { useState, useCallback } from 'react';
import { ConversationType } from '../components/ConversationTypeToggle';

interface UseChatStateReturn {
  selectedConversation: string;
  activeConversationType: ConversationType;
  isMobileSidebarOpen: boolean;
  setSelectedConversation: (conversationId: string) => void;
  setActiveConversationType: (type: ConversationType) => void;
  setIsMobileSidebarOpen: (isOpen: boolean) => void;
  handleConversationSelect: (conversationId: string) => void;
  handleConversationTypeChange: (type: ConversationType) => void;
  toggleMobileSidebar: () => void;
}

export const useChatState = (): UseChatStateReturn => {
  const [selectedConversation, setSelectedConversation] = useState<string>('');
  const [activeConversationType, setActiveConversationType] = useState<ConversationType>('groups');
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

  const handleConversationSelect = useCallback((conversationId: string) => {
    setSelectedConversation(conversationId);
    setIsMobileSidebarOpen(false); // Close sidebar on mobile after selection
  }, []);

  const handleConversationTypeChange = useCallback((type: ConversationType) => {
    setActiveConversationType(type);
  }, []);

  const toggleMobileSidebar = useCallback(() => {
    setIsMobileSidebarOpen(prev => !prev);
  }, []);

  return {
    selectedConversation,
    activeConversationType,
    isMobileSidebarOpen,
    setSelectedConversation,
    setActiveConversationType,
    setIsMobileSidebarOpen,
    handleConversationSelect,
    handleConversationTypeChange,
    toggleMobileSidebar
  };
};
import React, { useState, useEffect, useMemo } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../hooks/useAuth';
import { useChatState } from '../hooks/useChatState';
import { useConversations } from '../hooks/useConversations';
import { useUnreadMessages } from '../hooks/useUnreadMessages';
import { useMessageSearch } from '../hooks/useMessageSearch';
import { useUserSearch } from '../hooks/useUserSearch';
import { MessageType } from '../types/chat';
import UserSearchModal from '../components/UserSearchModal';
import { User } from '../types/chat';
import { ConversationType } from '../components/ConversationTypeToggle';
import { CreateGroupModal } from '../components/groups/CreateGroupModal';
import { GroupSettingsModal } from '../components/groups/GroupSettingsModal';
import Footer from '../components/Footer';
import UserSettingsModal from '../components/UserSettingsModal';
import { api } from '../services/api';
import ChatSidebar from '../components/layout/ChatSidebar';
import ChatMainArea from '../components/layout/ChatMainArea';

const ChatPage: React.FC = () => {
  const { messages, sendMessage, sendMessageStatusUpdate, isConnected, loadConversationMessages, isLoadingMessages, isReconnecting } = useWebSocket();
  const { user, logout } = useAuth();

  // Modal states
  const [isCreateGroupModalOpen, setIsCreateGroupModalOpen] = useState(false);
  const [isGroupSettingsModalOpen, setIsGroupSettingsModalOpen] = useState(false);
  const [isUserSettingsModalOpen, setIsUserSettingsModalOpen] = useState(false);

  // Custom hooks for state management
  const chatState = useChatState();
  const conversationHook = useConversations();
  const unreadHook = useUnreadMessages({
    messages,
    currentUserId: user?.id,
    selectedConversationId: chatState.selectedConversation
  });
  const searchHook = useMessageSearch();
  const userSearchHook = useUserSearch();

  // Auto-select first available conversation of the active type if none selected and preload its messages
  useEffect(() => {
    if (!chatState.selectedConversation && conversationHook.conversations.length > 0) {
      // Filter conversations based on active type
      const filteredConversations = conversationHook.conversations.filter(conv => {
        if (chatState.activeConversationType === 'groups') {
          return conv.type === 'GROUP';
        } else {
          return conv.type === 'DIRECT';
        }
      });

      console.log('🔍 Auto-selecting conversation:', {
        activeType: chatState.activeConversationType,
        totalConversations: conversationHook.conversations.length,
        filteredConversations: filteredConversations.length
      });

      if (filteredConversations.length > 0) {
        const firstConversation = filteredConversations[0];
        chatState.setSelectedConversation(firstConversation.id);

        // Immediately start loading messages for the first conversation to reduce perceived loading time
        if (user) {
          console.log('🚀 Preloading messages for first filtered conversation:', firstConversation.id, 'type:', firstConversation.type);
          loadConversationMessages(firstConversation.id);
        }
      }
    }
  }, [chatState.selectedConversation, conversationHook.conversations, chatState.activeConversationType, chatState, user, loadConversationMessages]);

  // Load messages for selected conversation (if not already loaded)
  useEffect(() => {
    if (chatState.selectedConversation && user) {
      loadConversationMessages(chatState.selectedConversation);
    }
  }, [chatState.selectedConversation, user, loadConversationMessages]);

  // Derived state with memoization
  const conversationMessages = useMemo(() =>
    messages.filter(msg => msg.conversationId === chatState.selectedConversation),
    [messages, chatState.selectedConversation]
  );

  // Automatic read receipt handling - mark messages as read when viewing conversation
  useEffect(() => {
    if (chatState.selectedConversation && user && conversationMessages.length > 0 && isConnected) {
      // Small delay to ensure messages are rendered and user has "seen" them
      const markAsReadTimer = setTimeout(() => {
        conversationMessages
          .filter(msg =>
            msg.senderId !== user.id && // Don't mark own messages as read
            !msg.readBy?.[user.id]      // Only mark if not already read by this user
          )
          .forEach(msg => {
            console.log(`Marking message ${msg.id} as read by user ${user.id}`);
            sendMessageStatusUpdate({
              messageId: msg.id,
              userId: user.id,
              statusType: 'READ',
              timestamp: new Date().toISOString()
            });
          });
      }, 1000); // 1 second delay to ensure user has "seen" the messages

      return () => clearTimeout(markAsReadTimer);
    }
  }, [chatState.selectedConversation, conversationMessages, user, isConnected, sendMessageStatusUpdate]);

  // Event handlers
  const handleSendMessage = (content: string) => {
    if (!content.trim() || !user || !chatState.selectedConversation) return;

    sendMessage({
      conversationId: chatState.selectedConversation,
      senderId: user.id,
      senderUsername: user.displayName || user.username, // Use displayName or username
      content: content.trim(),
      type: MessageType.TEXT,
    });
  };

  const handleConversationSelect = async (conversationId: string) => {
    chatState.handleConversationSelect(conversationId);

    // Load conversation messages when switching conversations
    await loadConversationMessages(conversationId);

    // Clear search when switching conversations
    searchHook.clearSearch();
    if (searchHook.isSearchMode) {
      searchHook.toggleSearchMode();
    }
  };

  const handleConversationTypeChange = (type: ConversationType) => {
    chatState.handleConversationTypeChange(type);

    // Clear search when switching types
    searchHook.clearSearch();
    if (searchHook.isSearchMode) {
      searchHook.toggleSearchMode();
    }
  };

  const handleUserSelect = async (selectedUser: User) => {
    try {
      await userSearchHook.createDirectConversation(selectedUser, (conversation) => {
        // Add conversation to list
        conversationHook.addConversation(conversation);

        // Switch to direct messages tab and select the conversation
        chatState.setActiveConversationType('direct');
        chatState.setSelectedConversation(conversation.id);

        // Load messages for the new conversation
        loadConversationMessages(conversation.id);
      });
    } catch (error) {
      console.error('Failed to create direct conversation:', error);
      alert('Failed to create direct conversation: ' + (error instanceof Error ? error.message : String(error)));
    }
  };

  const handleNewGroup = () => {
    setIsCreateGroupModalOpen(true);
  };

  const handleGroupCreated = (group: any) => {
    // Add group to conversation list
    conversationHook.addConversation(group);

    // Switch to groups tab and select the new group
    chatState.setActiveConversationType('groups');
    chatState.setSelectedConversation(group.id);

    // Load messages for the new group
    loadConversationMessages(group.id);

    // Close modal
    setIsCreateGroupModalOpen(false);
  };

  const handleDeleteConversation = async (conversationId: string) => {
    try {
      await api.conversation.deleteConversation(conversationId);

      // Remove conversation from the list
      conversationHook.removeConversation(conversationId);

      // If this was the selected conversation, clear the selection
      if (chatState.selectedConversation === conversationId) {
        chatState.setSelectedConversation('');
      }
    } catch (error) {
      console.error('Failed to delete conversation:', error);
      alert('Failed to delete conversation: ' + (error instanceof Error ? error.message : String(error)));
    }
  };

  // Helper functions
  const getCurrentConversation = () => {
    return conversationHook.conversations.find(conv => conv.id === chatState.selectedConversation);
  };

  const isCurrentConversationGroup = () => {
    const conversation = getCurrentConversation();
    return conversation?.type === 'GROUP';
  };

  const getCurrentUserRole = () => {
    const conversation = getCurrentConversation();
    if (!conversation || !conversation.participants || !user) return null;

    // Handle both ConversationDto (with ConversationParticipant[]) and Conversation (with User[])
    if (conversation.participants.length > 0 && 'user' in conversation.participants[0]) {
      // ConversationDto type with ConversationParticipant[]
      const participant = conversation.participants.find((p: any) => p.user.id === user.id) as any;
      return participant?.role || null;
    } else {
      // Conversation type with User[] - no role information available
      return null;
    }
  };

  // Helper function to get conversation display name
  const getConversationDisplayName = (conversationId: string) => {
    const conversation = conversationHook.conversations.find(conv => conv.id === conversationId);
    if (conversation) {
      if (conversation.type === 'DIRECT' && conversation.participants && conversation.participants.length > 0) {
        // Handle both ConversationDto (with ConversationParticipant[]) and Conversation (with User[])
        if ('user' in conversation.participants[0]) {
          // ConversationDto type with ConversationParticipant[]
          const otherParticipant = conversation.participants.find((p: any) => p.user.id !== user?.id) as any;
          if (otherParticipant) {
            const userObj = otherParticipant.user;
            return userObj.displayNameOrUsername || userObj.displayName || userObj.username || 'Unknown User';
          }
        } else {
          // Legacy format - treat as User objects (shouldn't happen with current backend)
          const otherParticipant = (conversation.participants as any[]).find((participant: any) => participant.id !== user?.id);
          if (otherParticipant) {
            return otherParticipant.displayNameOrUsername || otherParticipant.displayName || otherParticipant.username || 'Unknown User';
          }
        }
        return 'Unknown User';
      }
      return conversation.name;
    }

    // Fallback - return the conversation ID itself
    return conversationId;
  };

  const handleGroupUpdated = (updatedGroup: any) => {
    conversationHook.updateConversation(updatedGroup.id, updatedGroup);
  };

  return (
    <div className="h-screen flex flex-col bg-gray-50 dark:bg-gray-900 overflow-hidden">
      <div className="flex flex-1 relative min-h-0">
        <ChatSidebar
          isMobileSidebarOpen={chatState.isMobileSidebarOpen}
          setIsMobileSidebarOpen={chatState.setIsMobileSidebarOpen}
          user={user}
          logout={logout}
          isConnected={isConnected}
          isReconnecting={isReconnecting}
          selectedConversation={chatState.selectedConversation}
          handleConversationSelect={handleConversationSelect}
          conversations={conversationHook.conversations}
          activeConversationType={chatState.activeConversationType}
          handleConversationTypeChange={handleConversationTypeChange}
          onNewDirectMessage={userSearchHook.openUserSearchModal}
          onNewGroup={handleNewGroup}
          unreadCounts={unreadHook.unreadCounts}
          handleDeleteConversation={handleDeleteConversation}
          setIsUserSettingsModalOpen={setIsUserSettingsModalOpen}
        />

        <ChatMainArea
          isSearchMode={searchHook.isSearchMode}
          toggleSearchMode={searchHook.toggleSearchMode}
          setIsMobileSidebarOpen={chatState.setIsMobileSidebarOpen}
          selectedConversation={chatState.selectedConversation}
          conversationMessages={conversationMessages}
          isLoadingMessages={isLoadingMessages}
          isConnected={isConnected}
          handleSendMessage={handleSendMessage}
          searchResult={searchHook.searchResult}
          isSearchLoading={searchHook.isSearchLoading}
          searchError={searchHook.searchError}
          jumpToMessage={searchHook.jumpToMessage}
          loadMoreResults={searchHook.loadMoreResults}
          performSearch={searchHook.performSearch}
          clearSearch={searchHook.clearSearch}
          isCurrentConversationGroup={isCurrentConversationGroup()}
          setIsGroupSettingsModalOpen={setIsGroupSettingsModalOpen}
          getConversationDisplayName={getConversationDisplayName}
          user={user}
          isLoadingConversations={conversationHook.isLoading}
        />

        {/* User Search Modal */}
        <UserSearchModal
          isOpen={userSearchHook.isUserSearchModalOpen}
          onClose={userSearchHook.closeUserSearchModal}
          onSelectUser={handleUserSelect}
          searchUsers={userSearchHook.searchUsers}
          getUserSuggestions={userSearchHook.getUserSuggestions}
          currentUserId={user?.id}
        />

        {/* Create Group Modal */}
        <CreateGroupModal
          isOpen={isCreateGroupModalOpen}
          onClose={() => setIsCreateGroupModalOpen(false)}
          onGroupCreated={handleGroupCreated}
        />

        {/* Group Settings Modal */}
        {getCurrentConversation() && isCurrentConversationGroup() && (
          <GroupSettingsModal
            isOpen={isGroupSettingsModalOpen}
            onClose={() => setIsGroupSettingsModalOpen(false)}
            conversation={getCurrentConversation() as any}
            userRole={getCurrentUserRole() || 'MEMBER'}
            onGroupUpdated={handleGroupUpdated}
            onGroupDeleted={() => {
              handleDeleteConversation(getCurrentConversation()!.id);
              setIsGroupSettingsModalOpen(false);
            }}
          />
        )}

        {/* User Settings Modal */}
        <UserSettingsModal
          isOpen={isUserSettingsModalOpen}
          onClose={() => setIsUserSettingsModalOpen(false)}
        />
      </div>

      {/* Footer - Fixed at Bottom */}
      <div className="flex-shrink-0">
        <Footer />
      </div>
    </div>
  );
};

export default ChatPage;
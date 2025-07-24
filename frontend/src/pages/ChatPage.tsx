import React, { useState, useEffect, useMemo } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../hooks/useAuth';
import { useChatState } from '../hooks/useChatState';
import { useConversations } from '../hooks/useConversations';
import { useUnreadMessages } from '../hooks/useUnreadMessages';
import { useMessageSearch } from '../hooks/useMessageSearch';
import { useUserSearch } from '../hooks/useUserSearch';
import { ChatMessage, MessageType } from '../types/chat';
import MessageList from '../components/MessageList';
import MessageInput from '../components/MessageInput';
import ConversationList from '../components/ConversationList';
import UserSearchModal from '../components/UserSearchModal';
import { User } from '../types/chat';
import MessageSearchBar from '../components/MessageSearchBar';
import SearchResultsList from '../components/SearchResultsList';
import { ConversationType } from '../components/ConversationTypeToggle';
import { CreateGroupModal } from '../components/groups/CreateGroupModal';
import { GroupSettingsModal } from '../components/groups/GroupSettingsModal';
import { api } from '../services/api';

const ChatPage: React.FC = () => {
  const { messages, sendMessage, isConnected, loadConversationMessages, isLoadingMessages } = useWebSocket();
  const { user, logout } = useAuth();
  
  // Group modal states
  const [isCreateGroupModalOpen, setIsCreateGroupModalOpen] = useState(false);
  const [isGroupSettingsModalOpen, setIsGroupSettingsModalOpen] = useState(false);
  
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
  
  // Auto-select first available conversation if none selected
  useEffect(() => {
    if (!chatState.selectedConversation && conversationHook.conversations.length > 0) {
      const firstConversation = conversationHook.conversations[0];
      chatState.setSelectedConversation(firstConversation.id);
    }
  }, [chatState.selectedConversation, conversationHook.conversations, chatState]);

  // Load messages for initially selected conversation
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
    <div className="flex h-screen bg-white relative">
      {/* Mobile Sidebar Overlay */}
      {chatState.isMobileSidebarOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => chatState.setIsMobileSidebarOpen(false)}
        />
      )}

      {/* Sidebar - Conversations */}
      <div className={`
        fixed lg:relative lg:translate-x-0 z-50 lg:z-0
        w-80 lg:w-72 xl:w-80 bg-gray-50 border-r border-gray-200 h-full
        transition-transform duration-300 ease-in-out
        ${chatState.isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h1 className="text-lg lg:text-xl font-semibold text-gray-900">Chat Platform</h1>
            <div className="flex items-center space-x-2">
              <button
                onClick={logout}
                className="text-sm text-gray-500 hover:text-gray-700"
              >
                Logout
              </button>
              <button
                onClick={() => chatState.setIsMobileSidebarOpen(false)}
                className="lg:hidden p-1 text-gray-500 hover:text-gray-700"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
          <div className="flex items-center mt-2">
            <div className={`w-3 h-3 rounded-full mr-2 ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
            <span className="text-sm text-gray-600">
              {isConnected ? 'Connected' : 'Disconnected'}
            </span>
          </div>
          {user && (
            <div className="mt-2 text-sm text-gray-600">
              Welcome, {user.displayName || user.username}
            </div>
          )}
        </div>
        
        <ConversationList 
          selectedConversation={chatState.selectedConversation}
          onSelectConversation={handleConversationSelect}
          conversations={conversationHook.conversations}
          activeType={chatState.activeConversationType}
          onTypeChange={handleConversationTypeChange}
          onNewDirectMessage={userSearchHook.openUserSearchModal}
          onNewGroup={handleNewGroup}
          unreadCounts={unreadHook.unreadCounts}
          currentUserId={user?.id}
          onDeleteConversation={handleDeleteConversation}
        />
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex lg:ml-0">
        {/* Chat Column */}
        <div className={`flex flex-col transition-all duration-300 ${
          searchHook.isSearchMode ? 'lg:w-1/2' : 'w-full'
        }`}>
          {/* Chat Header */}
          <div className="p-4 border-b border-gray-200 bg-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center min-w-0 flex-1">
                <button
                  onClick={() => chatState.setIsMobileSidebarOpen(true)}
                  className="lg:hidden p-2 -ml-2 text-gray-500 hover:text-gray-700 mr-2 flex-shrink-0"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                  </svg>
                </button>
                
                <div className="min-w-0 flex-1">
                  <h2 className="text-lg font-medium text-gray-900 truncate">
                    {chatState.selectedConversation ? getConversationDisplayName(chatState.selectedConversation) : 'No conversation selected'}
                  </h2>
                  <div className="text-sm text-gray-500">
                    {conversationMessages.length} messages
                  </div>
                </div>
              </div>
              
              <div className="flex items-center space-x-2 flex-shrink-0">
                {/* Group Settings Button */}
                {isCurrentConversationGroup() && (
                  <button
                    onClick={() => setIsGroupSettingsModalOpen(true)}
                    className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-md"
                    title="Group Settings"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </button>
                )}
                
                <MessageSearchBar
                  isSearchMode={searchHook.isSearchMode}
                  onToggleSearch={searchHook.toggleSearchMode}
                  onSearch={(query) => searchHook.performSearch(chatState.selectedConversation, query)}
                  onClearSearch={searchHook.clearSearch}
                  isLoading={searchHook.isSearchLoading}
                  resultsCount={searchHook.searchResult?.totalCount}
                />
              </div>
            </div>
          </div>

          {/* Messages */}
          <MessageList messages={conversationMessages} currentUserId={user?.id} isLoading={isLoadingMessages} />

          {/* Message Input */}
          <div className="border-t border-gray-200 bg-white">
            <MessageInput 
              key={chatState.selectedConversation} 
              onSendMessage={handleSendMessage} 
              disabled={!isConnected || !chatState.selectedConversation} 
            />
          </div>
        </div>

        {/* Search Results Panel */}
        {searchHook.isSearchMode && (
          <div className="hidden lg:flex lg:w-1/2 border-l border-gray-200">
            <SearchResultsList
              searchResult={searchHook.searchResult}
              isLoading={searchHook.isSearchLoading}
              error={searchHook.searchError}
              onJumpToMessage={searchHook.jumpToMessage}
              onLoadMore={searchHook.searchResult?.hasMore ? () => searchHook.loadMoreResults(chatState.selectedConversation) : undefined}
              className="w-full"
            />
          </div>
        )}
      </div>

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
      
    </div>
  );
};

export default ChatPage;
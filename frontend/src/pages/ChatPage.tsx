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
import UserSearchModal, { User } from '../components/UserSearchModal';
import MessageSearchBar from '../components/MessageSearchBar';
import SearchResultsList from '../components/SearchResultsList';
import NetworkDebug from '../components/NetworkDebug';
import { ConversationType } from '../components/ConversationTypeToggle';

const ChatPage: React.FC = () => {
  const { messages, sendMessage, isConnected } = useWebSocket();
  const { user, logout } = useAuth();
  
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
  
  // Derived state with memoization
  const conversationMessages = useMemo(() => 
    messages.filter(msg => msg.conversationId === chatState.selectedConversation),
    [messages, chatState.selectedConversation]
  );

  // Event handlers
  const handleSendMessage = (content: string) => {
    if (!content.trim() || !user) return;

    sendMessage({
      conversationId: chatState.selectedConversation,
      senderId: user.id,
      senderUsername: undefined, // Backend will set this as source of truth
      content: content.trim(),
      type: MessageType.TEXT,
    });
  };

  const handleConversationSelect = (conversationId: string) => {
    chatState.handleConversationSelect(conversationId);
    
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
      });
    } catch (error) {
      console.error('Failed to create direct conversation:', error);
      alert('Failed to create direct conversation: ' + (error instanceof Error ? error.message : String(error)));
    }
  };

  // Helper function to get conversation display name
  const getConversationDisplayName = (conversationId: string) => {
    const conversation = conversationHook.conversations.find(conv => conv.id === conversationId);
    if (conversation) {
      if (conversation.type === 'DIRECT' && conversation.participants) {
        const otherParticipant = conversation.participants.find(p => p.id !== user?.id);
        return otherParticipant?.displayName || otherParticipant?.username || 'Unknown User';
      }
      return conversation.name;
    }
    
    // Fallback for default conversations
    switch (conversationId) {
      case 'general': return 'General Chat';
      case 'random': return 'Random';
      case 'tech': return 'Tech Talk';
      default: return conversationId;
    }
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
          unreadCounts={unreadHook.unreadCounts}
          currentUserId={user?.id}
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
                    {getConversationDisplayName(chatState.selectedConversation)}
                  </h2>
                  <div className="text-sm text-gray-500">
                    {conversationMessages.length} messages
                  </div>
                </div>
              </div>
              
              <div className="flex items-center space-x-2 flex-shrink-0">
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
          <MessageList messages={conversationMessages} currentUserId={user?.id} />

          {/* Message Input */}
          <div className="border-t border-gray-200 bg-white">
            <MessageInput 
              key={chatState.selectedConversation} 
              onSendMessage={handleSendMessage} 
              disabled={!isConnected} 
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
      />
      
      <NetworkDebug />
    </div>
  );
};

export default ChatPage;
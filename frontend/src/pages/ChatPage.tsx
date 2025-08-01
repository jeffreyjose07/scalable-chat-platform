import React, { useState, useEffect, useMemo, useRef } from 'react';
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
import NewMessageSearchBar from '../components/NewMessageSearchBar';
import SearchResultsList from '../components/SearchResultsList';
import { ConversationType } from '../components/ConversationTypeToggle';
import { CreateGroupModal } from '../components/groups/CreateGroupModal';
import { GroupSettingsModal } from '../components/groups/GroupSettingsModal';
import ThemeToggle from '../components/ThemeToggle';
import VersionInfo from '../components/VersionInfo';
import Footer from '../components/Footer';
import UserSettingsModal from '../components/UserSettingsModal';
import { api } from '../services/api';

const ChatPage: React.FC = () => {
  const { messages, sendMessage, sendMessageStatusUpdate, isConnected, loadConversationMessages, isLoadingMessages, isReconnecting, clearMessagesCache } = useWebSocket();
  const { user, logout } = useAuth();
  
  // Modal states
  const [isCreateGroupModalOpen, setIsCreateGroupModalOpen] = useState(false);
  const [isGroupSettingsModalOpen, setIsGroupSettingsModalOpen] = useState(false);
  const [isUserSettingsModalOpen, setIsUserSettingsModalOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  
  // Close user menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);
  
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
        w-80 lg:w-72 xl:w-80 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 
        ${chatState.isMobileSidebarOpen ? 'h-screen' : 'h-full'}
        flex flex-col shadow-lg lg:shadow-none
        transition-transform duration-300 ease-in-out
        ${chatState.isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
        <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex-shrink-0 bg-white dark:bg-gray-800">
          <div className="flex items-center justify-between">
            <h1 className="text-lg lg:text-xl font-bold text-gray-900 dark:text-gray-100 flex items-center">
              <div className="w-8 h-8 bg-gradient-to-br from-green-500 to-green-600 rounded-lg flex items-center justify-center mr-2 shadow-md">
                <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M2 5a2 2 0 012-2h7a2 2 0 012 2v4a2 2 0 01-2 2H9l-3 3v-3H4a2 2 0 01-2-2V5z" />
                  <path d="M15 7v2a4 4 0 01-4 4H9.828l-1.766 1.767c.28.149.599.233.938.233h2l3 3v-3h2a2 2 0 002-2V9a2 2 0 00-2-2h-1z" />
                </svg>
              </div>
              Chat Platform
              <VersionInfo className="ml-2" clickable />
            </h1>
            <div className="flex items-center space-x-2">
              <ThemeToggle />
              
              {/* User Menu Dropdown */}
              <div className="relative" ref={userMenuRef}>
                <button
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className="flex items-center space-x-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-white/70 dark:hover:bg-gray-600/70 px-3 py-2 rounded-lg transition-colors"
                  title="User Menu"
                >
                  {user && (() => {
                    const displayName = user.displayName || user.username;
                    const hue = displayName.charCodeAt(0) * 7 % 360;
                    const saturation = 75;
                    const lightness = 50;
                    const avatarColor = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
                    
                    return (
                      <div 
                        className="w-6 h-6 rounded-full flex items-center justify-center text-white text-xs font-semibold shadow-sm"
                        style={{ backgroundColor: avatarColor }}
                      >
                        {displayName.charAt(0).toUpperCase()}
                      </div>
                    );
                  })()}
                  <svg className={`w-4 h-4 transition-transform ${isUserMenuOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                {/* Dropdown Menu */}
                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-2 z-50">
                    {/* User Info */}
                    <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700">
                      <div className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {user?.displayName || user?.username}
                      </div>
                      <div className="text-xs text-gray-500 dark:text-gray-400">
                        {user?.email}
                      </div>
                    </div>
                    
                    {/* Menu Items */}
                    <button
                      onClick={() => {
                        setIsUserSettingsModalOpen(true);
                        setIsUserMenuOpen(false);
                      }}
                      className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center space-x-2"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      <span>Settings</span>
                    </button>
                    
                    <div className="border-t border-gray-200 dark:border-gray-700 my-1"></div>
                    
                    <button
                      onClick={() => {
                        logout();
                        setIsUserMenuOpen(false);
                      }}
                      className="w-full text-left px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center space-x-2"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                      </svg>
                      <span>Sign Out</span>
                    </button>
                  </div>
                )}
              </div>
              
              <button
                onClick={() => {
                  chatState.setIsMobileSidebarOpen(false);
                  setIsUserMenuOpen(false);
                }}
                className="lg:hidden p-1 text-gray-500 hover:text-gray-700 hover:bg-white/50 rounded-md transition-colors"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
          <div className="flex items-center mt-3">
            <div className={`w-3 h-3 rounded-full mr-2 shadow-sm ${
              isConnected ? 'bg-green-500 animate-pulse' : 
              isReconnecting ? 'bg-yellow-500 animate-spin' : 'bg-red-500'
            }`}></div>
            <span className={`text-sm font-medium ${
              isConnected ? 'text-green-700 dark:text-green-400' : 
              isReconnecting ? 'text-yellow-600 dark:text-yellow-400' : 'text-red-600 dark:text-red-400'
            }`}>
              {isConnected ? 'Connected' : isReconnecting ? 'Reconnecting...' : 'Disconnected'}
            </span>
          </div>
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
      <div className="flex-1 flex lg:ml-0 min-h-0">
        {/* Chat Column */}
        <div className={`flex flex-col transition-all duration-300 min-h-0 w-full ${
          searchHook.isSearchMode ? 'lg:w-1/2' : 'w-full'
        }`}>
          {/* Chat Header - Fixed at top */}
          <div className="px-4 sm:px-6 py-3 border-b border-gray-200 dark:border-gray-700 bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm z-10 sticky top-0">
            <div className="flex items-center justify-between">
              <div className="flex items-center min-w-0 flex-1">
                <button
                  onClick={() => chatState.setIsMobileSidebarOpen(true)}
                  className="lg:hidden p-2 -ml-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg mr-2 flex-shrink-0 transition-colors"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                  </svg>
                </button>
                
                <div className="min-w-0 flex-1">
                  <div className="flex items-center">
                    {chatState.selectedConversation && (() => {
                      const displayName = getConversationDisplayName(chatState.selectedConversation);
                      const hue = displayName.charCodeAt(0) * 7 % 360;
                      const saturation = 75;
                      const lightness = 45;
                      const avatarColor = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
                      
                      return (
                        <div 
                          className="w-10 h-10 rounded-full flex items-center justify-center text-white font-semibold mr-3 shadow-md border-2 border-white/20 dark:border-gray-600/30"
                          style={{ background: `linear-gradient(135deg, ${avatarColor}, hsl(${hue}, ${saturation}%, ${lightness - 10}%))` }}
                        >
                          {displayName.charAt(0).toUpperCase()}
                        </div>
                      );
                    })()}
                    <div>
                      <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 truncate">
                        {chatState.selectedConversation ? getConversationDisplayName(chatState.selectedConversation) : 'No conversation selected'}
                      </h2>
                      <div className="text-sm text-gray-500 dark:text-gray-400 flex items-center">
                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                        </svg>
                        {conversationMessages.length} messages
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="flex items-center space-x-2 flex-shrink-0 relative">
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
                
                <NewMessageSearchBar
                  isSearchMode={searchHook.isSearchMode}
                  onToggleSearch={searchHook.toggleSearchMode}
                  onSearch={(query, filters) => {
                    console.log('🔍 ChatPage onSearch called:', { 
                      query, 
                      filters, 
                      selectedConversationId: chatState.selectedConversation 
                    });
                    
                    if (!chatState.selectedConversation) {
                      console.error('❌ No conversation selected for search');
                      return;
                    }
                    
                    searchHook.performSearch(chatState.selectedConversation, query, filters);
                  }}
                  onClearSearch={searchHook.clearSearch}
                  isLoading={searchHook.isSearchLoading}
                  resultsCount={searchHook.searchResult?.totalCount}
                  enableFilters={true}
                />
              </div>
            </div>
          </div>

          {/* Messages - Scrollable Area */}
          <div className="flex-1 overflow-y-auto -mt-16 pt-16 pb-4 px-4 sm:px-6" style={{
            WebkitOverflowScrolling: 'touch',
            scrollBehavior: 'smooth',
            scrollPaddingTop: '4rem',
            scrollPaddingBottom: '4rem'
          }}>
            <MessageList 
              messages={conversationMessages} 
              currentUserId={user?.id} 
              isLoading={isLoadingMessages || conversationHook.isLoading || !isConnected} 
            />
          </div>

          {/* Message Input - Fixed at Bottom */}
          <div className="flex-shrink-0 border-t border-gray-200 dark:border-gray-700 bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm sticky bottom-0 z-10">
            <MessageInput 
              key={chatState.selectedConversation} 
              onSendMessage={handleSendMessage} 
              disabled={!isConnected || !chatState.selectedConversation} 
            />
          </div>
        </div>

        {/* Search Results Panel - Desktop: Side panel, Mobile: Overlay */}
        {searchHook.isSearchMode && (
          <>
            {/* Desktop: Side panel */}
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
            
            {/* Mobile: Full screen overlay */}
            <div className="lg:hidden fixed inset-0 bg-white z-50 flex flex-col">
              {/* Mobile search header */}
              <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-white">
                <h2 className="text-lg font-semibold text-gray-900">Search Results</h2>
                <button
                  onClick={searchHook.toggleSearchMode}
                  className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              
              {/* Search results content */}
              <div className="flex-1 overflow-hidden">
                <SearchResultsList
                  searchResult={searchHook.searchResult}
                  isLoading={searchHook.isSearchLoading}
                  error={searchHook.searchError}
                  onJumpToMessage={(messageId) => {
                    searchHook.jumpToMessage(messageId);
                    // Close search on mobile after jumping
                    searchHook.toggleSearchMode();
                  }}
                  onLoadMore={searchHook.searchResult?.hasMore ? () => searchHook.loadMoreResults(chatState.selectedConversation) : undefined}
                  className="h-full"
                />
              </div>
            </div>
          </>
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
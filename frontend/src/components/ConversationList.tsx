import React from 'react';
import ConversationTypeToggle, { ConversationType as UIConversationType } from './ConversationTypeToggle';
import { Conversation, User, ConversationParticipant } from '../types/chat';

interface ConversationListProps {
  selectedConversation: string;
  onSelectConversation: (conversationId: string) => void;
  conversations?: Conversation[];
  activeType: UIConversationType;
  onTypeChange: (type: UIConversationType) => void;
  onNewDirectMessage: () => void;
  onNewGroup?: () => void;
  unreadCounts?: Record<string, number>;
  currentUserId?: string;
  onDeleteConversation?: (conversationId: string) => void;
}

const ConversationList: React.FC<ConversationListProps> = ({ 
  selectedConversation, 
  onSelectConversation,
  conversations = [],
  activeType,
  onTypeChange,
  onNewDirectMessage,
  onNewGroup,
  unreadCounts = {},
  currentUserId,
  onDeleteConversation
}) => {
  // Security fix: Never show fallback conversations to prevent unauthorized access
  // Only show conversations that the user is actually authorized to see
  const displayConversations = conversations;
  
  // Filter conversations based on active type
  const filteredConversations = displayConversations.filter(conv => {
    if (activeType === 'groups') {
      return conv.type === 'GROUP';
    } else {
      return conv.type === 'DIRECT';
    }
  });
  
  // Debug logging
  console.log('🔍 ConversationList Debug:', {
    activeType,
    totalConversations: displayConversations.length,
    filteredConversations: filteredConversations.length,
    allConversations: displayConversations.map(c => ({
      id: c.id, 
      name: c.name, 
      type: c.type,
      participants: c.participants?.map(p => ({
        user: p.user || p, // Handle both formats
        role: (p as any).role || 'N/A'
      }))
    })),
    filtered: filteredConversations.map(c => ({
      id: c.id, 
      name: c.name, 
      type: c.type,
      participants: c.participants?.map(p => ({
        user: p.user || p, // Handle both formats
        role: (p as any).role || 'N/A'
      }))
    })),
    currentUserId
  });

  const getConversationDisplayName = (conversation: Conversation) => {
    if (conversation.type === 'DIRECT' && conversation.participants && conversation.participants.length > 0) {
      // Backend returns ConversationParticipantDto objects with nested user property
      const otherParticipant = conversation.participants.find((participant: ConversationParticipant) => {
        return participant.user.id !== currentUserId;
      });
      
      if (otherParticipant) {
        const user = otherParticipant.user;
        return user.displayNameOrUsername || user.displayName || user.username || 'Unknown User';
      }
      return 'Unknown User';
    }
    return String(conversation.name || 'Unnamed Conversation');
  };

  const getConversationAvatar = (conversation: Conversation) => {
    const displayName = getConversationDisplayName(conversation);
    const safeDisplayName = String(displayName || 'U');
    const avatarColor = `hsl(${safeDisplayName.charCodeAt(0) * 7 % 360}, 70%, 55%)`;
    
    if (conversation.type === 'DIRECT') {
      return (
        <div className="relative mr-3 flex-shrink-0">
          <div 
            className="w-12 h-12 rounded-full flex items-center justify-center text-white font-semibold shadow-lg"
            style={{ background: `linear-gradient(135deg, ${avatarColor}, ${avatarColor}dd)` }}
          >
            <span className="text-lg">
              {safeDisplayName.charAt(0).toUpperCase()}
            </span>
          </div>
          {/* Online status indicator */}
          <div className="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-green-500 border-2 border-white rounded-full"></div>
        </div>
      );
    } else {
      return (
        <div className="relative mr-3 flex-shrink-0">
          <div 
            className="w-12 h-12 rounded-full flex items-center justify-center text-white shadow-lg"
            style={{ background: 'linear-gradient(135deg, #6366f1, #8b5cf6)' }}
          >
            <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
              <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z" />
            </svg>
          </div>
        </div>
      );
    }
  };

  const canDeleteConversation = (conversation: Conversation) => {
    // For direct conversations, any participant can delete (removes from their view)
    if (conversation.type === 'DIRECT') {
      return true;
    }
    
    // For groups, only owners can delete
    if (conversation.type === 'GROUP' && conversation.participants && currentUserId) {
      const currentUserParticipant = conversation.participants.find((participant: ConversationParticipant) => {
        return participant.user.id === currentUserId;
      });
      
      return currentUserParticipant && (currentUserParticipant as any).role === 'OWNER';
    }
    
    return false;
  };

  return (
    <div className="flex-1 overflow-y-auto">
      <div className="p-3 sm:p-4 pb-6">
        <ConversationTypeToggle 
          activeType={activeType}
          onTypeChange={onTypeChange}
        />
        
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold text-gray-700">
            {activeType === 'groups' ? 'Group Conversations' : 'Direct Messages'}
          </h3>
          
          {activeType === 'direct' && (
            <button
              onClick={onNewDirectMessage}
              className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
              title="New Direct Message"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </button>
          )}
          
          {activeType === 'groups' && onNewGroup && (
            <button
              onClick={onNewGroup}
              className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
              title="Create New Group"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </button>
          )}
        </div>
        
        <div className="space-y-1">
          {filteredConversations.length > 0 ? (
            filteredConversations.map((conversation) => (
              <div
                key={conversation.id}
                className={`group relative transition-all duration-200 ${
                  selectedConversation === conversation.id
                    ? 'bg-green-50 border-r-4 border-green-500 shadow-sm'
                    : 'hover:bg-gray-50 hover:shadow-sm'
                }`}
              >
                <button
                  onClick={() => onSelectConversation(conversation.id)}
                  className="w-full text-left p-4 transition-all duration-200"
                >
                  <div className="flex items-center">
                    {getConversationAvatar(conversation)}
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between mb-1">
                        <div className={`font-semibold truncate text-base ${
                          selectedConversation === conversation.id ? 'text-gray-900' : 'text-gray-800'
                        }`}>
                          {String(getConversationDisplayName(conversation))}
                        </div>
                        <div className="flex items-center space-x-2">
                          {conversation.lastMessage && (
                            <span className="text-xs text-gray-500">
                              {new Date().toDateString() === new Date(conversation.lastMessage.timestamp || Date.now()).toDateString() 
                                ? new Date(conversation.lastMessage.timestamp || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                                : new Date(conversation.lastMessage.timestamp || Date.now()).toLocaleDateString([], { month: 'short', day: 'numeric' })
                              }
                            </span>
                          )}
                          {unreadCounts[conversation.id] && unreadCounts[conversation.id] > 0 && (
                            <span className="bg-green-500 text-white text-xs rounded-full px-2.5 py-1 font-semibold shadow-sm">
                              {unreadCounts[conversation.id] > 99 ? '99+' : unreadCounts[conversation.id]}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center justify-between">
                        {conversation.lastMessage ? (
                          <div className={`text-sm truncate pr-2 ${
                            unreadCounts[conversation.id] && unreadCounts[conversation.id] > 0 
                              ? 'text-gray-700 font-medium' 
                              : 'text-gray-500'
                          }`}>
                            {typeof conversation.lastMessage === 'string' 
                              ? conversation.lastMessage 
                              : conversation.lastMessage.content
                            }
                          </div>
                        ) : null}
                        {/* Message status for sent messages */}
                        {conversation.lastMessage && conversation.lastMessage.senderId === currentUserId && (
                          <div className="flex items-center text-gray-400">
                            {/* Single checkmark for sent messages in conversation list */}
                            <svg className="w-3 h-3 opacity-70" fill="currentColor" viewBox="0 0 20 20">
                              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                            </svg>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </button>
                
                {/* Delete button - appears on hover, only for authorized users */}
                {onDeleteConversation && canDeleteConversation(conversation) && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      if (window.confirm(`Are you sure you want to delete this ${conversation.type === 'GROUP' ? 'group' : 'conversation'}?`)) {
                        onDeleteConversation(conversation.id);
                      }
                    }}
                    className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity p-1 text-red-500 hover:text-red-700 hover:bg-red-50 rounded"
                    title={`Delete ${conversation.type === 'GROUP' ? 'group' : 'conversation'}`}
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                )}
              </div>
            ))
          ) : (
            <div className="text-center py-8 text-gray-500">
              {activeType === 'groups' 
                ? 'No group conversations yet' 
                : 'No direct messages yet'}
              {activeType === 'direct' && (
                <div className="mt-2">
                  <button
                    onClick={onNewDirectMessage}
                    className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                  >
                    Start a conversation
                  </button>
                </div>
              )}
              {activeType === 'groups' && onNewGroup && (
                <div className="mt-2">
                  <button
                    onClick={onNewGroup}
                    className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                  >
                    Create a group
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ConversationList;
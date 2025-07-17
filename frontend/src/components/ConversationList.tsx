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
    console.log('Debug displayName:', displayName, 'type:', typeof displayName);
    const safeDisplayName = String(displayName || 'U'); // Ensure it's a string
    console.log('Debug safeDisplayName:', safeDisplayName, 'type:', typeof safeDisplayName);
    if (conversation.type === 'DIRECT') {
      // For direct messages, show user avatar (could be actual image later)
      return (
        <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center mr-3 flex-shrink-0">
          <span className="text-sm font-medium text-white">
            {safeDisplayName.charAt(0).toUpperCase()}
          </span>
        </div>
      );
    } else {
      // For groups, show group icon
      return (
        <div className="w-8 h-8 bg-gray-300 rounded-full flex items-center justify-center mr-3 flex-shrink-0">
          <svg className="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
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
      <div className="p-3 sm:p-4">
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
                className={`group relative rounded-lg transition-colors ${
                  selectedConversation === conversation.id
                    ? 'bg-blue-100 text-blue-700'
                    : 'hover:bg-gray-100 text-gray-700'
                }`}
              >
                <button
                  onClick={() => onSelectConversation(conversation.id)}
                  className="w-full text-left p-3 rounded-lg"
                >
                  <div className="flex items-center">
                    {getConversationAvatar(conversation)}
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between">
                        <div className="font-medium truncate">{String(getConversationDisplayName(conversation))}</div>
                        {unreadCounts[conversation.id] && unreadCounts[conversation.id] > 0 && (
                          <span className="bg-blue-500 text-white text-xs rounded-full px-2 py-1 ml-2">
                            {unreadCounts[conversation.id]}
                          </span>
                        )}
                      </div>
                      {conversation.lastMessage && (
                        <div className="text-sm text-gray-500 truncate mt-1">
                          {typeof conversation.lastMessage === 'string' ? conversation.lastMessage : conversation.lastMessage.content}
                        </div>
                      )}
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
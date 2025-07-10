import React from 'react';
import ConversationTypeToggle, { ConversationType } from './ConversationTypeToggle';

export interface Conversation {
  id: string;
  name: string;
  type: 'GROUP' | 'DIRECT';
  lastMessage?: string;
  unreadCount?: number;
  participants?: Array<{ id: string; username: string; displayName?: string }>;
  updatedAt?: string;
}

interface ConversationListProps {
  selectedConversation: string;
  onSelectConversation: (conversationId: string) => void;
  conversations?: Conversation[];
  activeType: ConversationType;
  onTypeChange: (type: ConversationType) => void;
  onNewDirectMessage: () => void;
  unreadCounts?: Record<string, number>;
  currentUserId?: string;
}

const ConversationList: React.FC<ConversationListProps> = ({ 
  selectedConversation, 
  onSelectConversation,
  conversations = [],
  activeType,
  onTypeChange,
  onNewDirectMessage,
  unreadCounts = {},
  currentUserId
}) => {
  // Default conversations for development
  const defaultConversations: Conversation[] = [
    { id: 'general', name: 'General Chat', type: 'GROUP', unreadCount: 0 },
    { id: 'random', name: 'Random', type: 'GROUP', unreadCount: 0 },
    { id: 'tech', name: 'Tech Talk', type: 'GROUP', unreadCount: 0 },
  ];

  const displayConversations = conversations.length > 0 ? conversations : defaultConversations;
  
  // Filter conversations based on active type
  const filteredConversations = displayConversations.filter(conv => {
    if (activeType === 'groups') {
      return conv.type === 'GROUP';
    } else {
      return conv.type === 'DIRECT';
    }
  });

  const getConversationDisplayName = (conversation: Conversation) => {
    if (conversation.type === 'DIRECT' && conversation.participants) {
      // For direct messages, show the other participant's name
      const otherParticipant = conversation.participants.find(p => p.id !== currentUserId);
      return otherParticipant?.displayName || otherParticipant?.username || 'Unknown User';
    }
    return conversation.name;
  };

  const getConversationAvatar = (conversation: Conversation) => {
    const displayName = getConversationDisplayName(conversation);
    if (conversation.type === 'DIRECT') {
      // For direct messages, show user avatar (could be actual image later)
      return (
        <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center mr-3 flex-shrink-0">
          <span className="text-sm font-medium text-white">
            {displayName.charAt(0).toUpperCase()}
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
        </div>
        
        <div className="space-y-1">
          {filteredConversations.length > 0 ? (
            filteredConversations.map((conversation) => (
              <button
                key={conversation.id}
                onClick={() => onSelectConversation(conversation.id)}
                className={`w-full text-left p-3 rounded-lg transition-colors ${
                  selectedConversation === conversation.id
                    ? 'bg-blue-100 text-blue-700'
                    : 'hover:bg-gray-100 text-gray-700'
                }`}
              >
                <div className="flex items-center">
                  {getConversationAvatar(conversation)}
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between">
                      <div className="font-medium truncate">{getConversationDisplayName(conversation)}</div>
                      {unreadCounts[conversation.id] && unreadCounts[conversation.id] > 0 && (
                        <span className="bg-blue-500 text-white text-xs rounded-full px-2 py-1 ml-2">
                          {unreadCounts[conversation.id]}
                        </span>
                      )}
                    </div>
                    {conversation.lastMessage && (
                      <div className="text-sm text-gray-500 truncate mt-1">
                        {conversation.lastMessage}
                      </div>
                    )}
                  </div>
                </div>
              </button>
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
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ConversationList;
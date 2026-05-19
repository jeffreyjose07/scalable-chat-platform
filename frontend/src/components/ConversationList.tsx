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
  const displayConversations = conversations;

  const filteredConversations = displayConversations.filter(conv =>
    activeType === 'groups' ? conv.type === 'GROUP' : conv.type === 'DIRECT'
  );

  console.log('🔍 ConversationList Debug:', {
    activeType,
    totalConversations: displayConversations.length,
    filteredConversations: filteredConversations.length,
  });

  const getConversationDisplayName = (conversation: Conversation) => {
    if (conversation.type === 'DIRECT' && conversation.participants && conversation.participants.length > 0) {
      const otherParticipant = conversation.participants.find(
        (participant: ConversationParticipant) => participant.user.id !== currentUserId
      );
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
    const safe = String(displayName || 'U');
    const hue = safe.charCodeAt(0) * 7 % 360;

    if (conversation.type === 'DIRECT') {
      return (
        <div className="relative mr-3 flex-shrink-0">
          <div
            className="w-11 h-11 rounded-full flex items-center justify-center text-white font-semibold shadow-sm text-base"
            style={{ background: `hsl(${hue}, 65%, 48%)` }}
          >
            {safe.charAt(0).toUpperCase()}
          </div>
        </div>
      );
    }
    return (
      <div className="relative mr-3 flex-shrink-0">
        <div className="w-11 h-11 rounded-full flex items-center justify-center text-white shadow-sm bg-amber-700">
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
            <path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3z" />
          </svg>
        </div>
      </div>
    );
  };

  const canDeleteConversation = (conversation: Conversation) => {
    if (conversation.type === 'DIRECT') return true;
    if (conversation.type === 'GROUP' && conversation.participants && currentUserId) {
      const currentUserParticipant = conversation.participants.find(
        (participant: ConversationParticipant) => participant.user.id === currentUserId
      );
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
          conversations={conversations}
          unreadCounts={unreadCounts}
        />

        <div className="flex items-center justify-between mb-2 px-1">
          <h3 className="text-xs font-semibold text-gray-400 dark:text-zinc-500 uppercase tracking-wider">
            {activeType === 'groups' ? 'Groups' : 'Direct Messages'}
          </h3>

          {activeType === 'direct' && (
            <button
              onClick={onNewDirectMessage}
              className="p-1.5 text-gray-400 dark:text-zinc-500 hover:text-amber-700 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-900/20 rounded-lg transition-colors"
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
              className="p-1.5 text-gray-400 dark:text-zinc-500 hover:text-amber-700 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-900/20 rounded-lg transition-colors"
              title="Create New Group"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </button>
          )}
        </div>

        <div className="space-y-0.5">
          {filteredConversations.length > 0 ? (
            filteredConversations.map((conversation) => {
              const isSelected = selectedConversation === conversation.id;
              const hasUnread = unreadCounts[conversation.id] && unreadCounts[conversation.id] > 0;

              return (
                <div
                  key={conversation.id}
                  className={`group relative transition-all duration-150 rounded-xl ${
                    isSelected
                      ? 'bg-amber-50 dark:bg-amber-900/20 ring-1 ring-amber-200 dark:ring-amber-800/40'
                      : 'hover:bg-gray-50 dark:hover:bg-zinc-800/60'
                  }`}
                >
                  <button
                    onClick={() => onSelectConversation(conversation.id)}
                    className="w-full text-left p-3 rounded-xl"
                  >
                    <div className="flex items-center">
                      {getConversationAvatar(conversation)}
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between mb-0.5">
                          <div className={`font-semibold truncate text-sm ${
                            isSelected
                              ? 'text-amber-900 dark:text-amber-200'
                              : 'text-gray-800 dark:text-gray-200'
                          }`}>
                            {String(getConversationDisplayName(conversation))}
                          </div>
                          <div className="flex items-center space-x-2 mr-7 flex-shrink-0">
                            {conversation.lastMessage && (
                              <span className="text-xs text-gray-400 dark:text-zinc-500">
                                {new Date().toDateString() === new Date(conversation.lastMessage.timestamp || Date.now()).toDateString()
                                  ? new Date(conversation.lastMessage.timestamp || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                                  : new Date(conversation.lastMessage.timestamp || Date.now()).toLocaleDateString([], { month: 'short', day: 'numeric' })
                                }
                              </span>
                            )}
                            {hasUnread && (
                              <span className="bg-amber-600 text-white text-xs rounded-full px-2 py-0.5 font-semibold shadow-sm min-w-[20px] text-center">
                                {unreadCounts[conversation.id] > 99 ? '99+' : unreadCounts[conversation.id]}
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="flex items-center justify-between">
                          {conversation.lastMessage ? (
                            <div className={`text-xs truncate pr-2 ${
                              hasUnread
                                ? 'text-gray-600 dark:text-gray-300 font-medium'
                                : 'text-gray-400 dark:text-zinc-500'
                            }`}>
                              {typeof conversation.lastMessage === 'string'
                                ? conversation.lastMessage
                                : conversation.lastMessage.content}
                            </div>
                          ) : null}
                          {conversation.lastMessage && conversation.lastMessage.senderId === currentUserId && (
                            <div className="flex items-center text-gray-300 dark:text-zinc-600 flex-shrink-0">
                              <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                                <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                              </svg>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </button>

                  {onDeleteConversation && canDeleteConversation(conversation) && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (window.confirm(`Are you sure you want to delete this ${conversation.type === 'GROUP' ? 'group' : 'conversation'}?`)) {
                          onDeleteConversation(conversation.id);
                        }
                      }}
                      className="absolute top-2.5 right-2 opacity-0 group-hover:opacity-100 transition-opacity p-1.5 text-gray-300 dark:text-zinc-600 hover:text-red-500 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg"
                      title={`Delete ${conversation.type === 'GROUP' ? 'group' : 'conversation'}`}
                    >
                      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                </div>
              );
            })
          ) : (
            <div className="text-center py-12">
              <div className="w-12 h-12 bg-gray-100 dark:bg-zinc-800 rounded-xl flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-gray-300 dark:text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <div className="text-gray-400 dark:text-zinc-500 text-sm font-medium">
                {activeType === 'groups' ? 'No groups yet' : 'No direct messages yet'}
              </div>
              <div className="mt-3">
                {activeType === 'direct' && (
                  <button
                    onClick={onNewDirectMessage}
                    className="text-amber-700 dark:text-amber-400 hover:text-amber-600 text-sm font-medium"
                  >
                    Start a conversation →
                  </button>
                )}
                {activeType === 'groups' && onNewGroup && (
                  <button
                    onClick={onNewGroup}
                    className="text-amber-700 dark:text-amber-400 hover:text-amber-600 text-sm font-medium"
                  >
                    Create a group →
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ConversationList;

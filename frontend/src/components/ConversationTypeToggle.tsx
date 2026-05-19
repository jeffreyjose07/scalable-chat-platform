import React from 'react';
import { Conversation } from '../types/chat';

export type ConversationType = 'groups' | 'direct';

interface ConversationTypeToggleProps {
  activeType: ConversationType;
  onTypeChange: (type: ConversationType) => void;
  conversations?: Conversation[];
  unreadCounts?: Record<string, number>;
}

const ConversationTypeToggle: React.FC<ConversationTypeToggleProps> = ({
  activeType,
  onTypeChange,
  conversations = [],
  unreadCounts = {}
}) => {
  const getUnreadCountForType = (type: 'groups' | 'direct') => {
    const targetType = type === 'groups' ? 'GROUP' : 'DIRECT';
    return conversations
      .filter(conv => conv.type === targetType)
      .reduce((total, conv) => total + (unreadCounts[conv.id] || 0), 0);
  };

  const groupsUnreadCount = getUnreadCountForType('groups');
  const directUnreadCount = getUnreadCountForType('direct');

  return (
    <div className="flex bg-gray-100 dark:bg-zinc-900 rounded-xl p-1 mb-4 border border-gray-200/60 dark:border-zinc-700/50">
      <button
        onClick={() => onTypeChange('groups')}
        className={`flex-1 px-3 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
          activeType === 'groups'
            ? 'bg-white dark:bg-zinc-700 text-green-600 dark:text-green-400 shadow-sm'
            : 'text-gray-500 dark:text-zinc-400 hover:text-gray-800 dark:hover:text-zinc-200'
        }`}
      >
        <div className="flex items-center justify-center space-x-2">
          <div className="relative">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            {groupsUnreadCount > 0 && (
              <span className="absolute -top-1.5 -right-1.5 bg-green-500 text-white text-xs rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 font-semibold shadow-sm">
                {groupsUnreadCount > 99 ? '99+' : groupsUnreadCount}
              </span>
            )}
          </div>
          <span>Groups</span>
        </div>
      </button>

      <button
        onClick={() => onTypeChange('direct')}
        className={`flex-1 px-3 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
          activeType === 'direct'
            ? 'bg-white dark:bg-zinc-700 text-green-600 dark:text-green-400 shadow-sm'
            : 'text-gray-500 dark:text-zinc-400 hover:text-gray-800 dark:hover:text-zinc-200'
        }`}
      >
        <div className="flex items-center justify-center space-x-2">
          <div className="relative">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            {directUnreadCount > 0 && (
              <span className="absolute -top-1.5 -right-1.5 bg-green-500 text-white text-xs rounded-full min-w-[16px] h-4 flex items-center justify-center px-1 font-semibold shadow-sm">
                {directUnreadCount > 99 ? '99+' : directUnreadCount}
              </span>
            )}
          </div>
          <span>Direct</span>
        </div>
      </button>
    </div>
  );
};

export default ConversationTypeToggle;

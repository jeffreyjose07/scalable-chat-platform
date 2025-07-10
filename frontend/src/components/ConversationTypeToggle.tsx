import React from 'react';

export type ConversationType = 'groups' | 'direct';

interface ConversationTypeToggleProps {
  activeType: ConversationType;
  onTypeChange: (type: ConversationType) => void;
}

const ConversationTypeToggle: React.FC<ConversationTypeToggleProps> = ({
  activeType,
  onTypeChange
}) => {
  return (
    <div className="flex bg-gray-100 rounded-lg p-1 mb-4">
      <button
        onClick={() => onTypeChange('groups')}
        className={`flex-1 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
          activeType === 'groups'
            ? 'bg-white text-blue-600 shadow-sm'
            : 'text-gray-600 hover:text-gray-900'
        }`}
      >
        <div className="flex items-center justify-center space-x-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          <span>Groups</span>
        </div>
      </button>
      
      <button
        onClick={() => onTypeChange('direct')}
        className={`flex-1 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
          activeType === 'direct'
            ? 'bg-white text-blue-600 shadow-sm'
            : 'text-gray-600 hover:text-gray-900'
        }`}
      >
        <div className="flex items-center justify-center space-x-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} 
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <span>Direct</span>
        </div>
      </button>
    </div>
  );
};

export default ConversationTypeToggle;
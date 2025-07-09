import React from 'react';

interface ConversationListProps {
  selectedConversation: string;
  onSelectConversation: (conversationId: string) => void;
}

const ConversationList: React.FC<ConversationListProps> = ({ 
  selectedConversation, 
  onSelectConversation 
}) => {
  const conversations = [
    { id: 'general', name: 'General Chat' },
    { id: 'random', name: 'Random' },
    { id: 'tech', name: 'Tech Talk' },
  ];

  return (
    <div className="flex-1 overflow-y-auto">
      <div className="p-3 sm:p-4">
        <h3 className="text-sm font-semibold text-gray-700 mb-3">Conversations</h3>
        <div className="space-y-1">
          {conversations.map((conversation) => (
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
                <div className="w-8 h-8 bg-gray-300 rounded-full flex items-center justify-center mr-3 flex-shrink-0">
                  <span className="text-sm font-medium text-gray-600">
                    {conversation.name.charAt(0).toUpperCase()}
                  </span>
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-medium truncate">{conversation.name}</div>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ConversationList;
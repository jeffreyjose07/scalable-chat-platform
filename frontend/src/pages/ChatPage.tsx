import React, { useState, useEffect } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../hooks/useAuth';
import { ChatMessage, MessageType } from '../types/chat';
import MessageList from '../components/MessageList';
import MessageInput from '../components/MessageInput';
import ConversationList from '../components/ConversationList';

const ChatPage: React.FC = () => {
  const { messages, sendMessage, isConnected } = useWebSocket();
  const { user, logout } = useAuth();
  const [selectedConversation, setSelectedConversation] = useState<string>('general');
  const [conversationMessages, setConversationMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    const filtered = messages.filter(msg => msg.conversationId === selectedConversation);
    setConversationMessages(filtered);
  }, [messages, selectedConversation]);

  const handleSendMessage = (content: string) => {
    if (!content.trim() || !user) return;

    sendMessage({
      conversationId: selectedConversation,
      senderId: user.id,
      senderUsername: undefined, // Backend will set this as source of truth
      content: content.trim(),
      type: MessageType.TEXT,
    });
  };

  return (
    <div className="flex h-screen bg-white">
      {/* Sidebar - Conversations */}
      <div className="w-80 bg-gray-50 border-r border-gray-200">
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h1 className="text-xl font-semibold text-gray-900">Chat Platform</h1>
            <button
              onClick={logout}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              Logout
            </button>
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
          selectedConversation={selectedConversation}
          onSelectConversation={setSelectedConversation}
        />
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col">
        {/* Chat Header */}
        <div className="p-4 border-b border-gray-200 bg-white">
          <h2 className="text-lg font-medium text-gray-900">
            {selectedConversation === 'general' ? 'General Chat' : 
             selectedConversation === 'random' ? 'Random' :
             selectedConversation === 'tech' ? 'Tech Talk' : selectedConversation}
          </h2>
          <div className="text-sm text-gray-500">
            {conversationMessages.length} messages
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-hidden">
          <MessageList messages={conversationMessages} currentUserId={user?.id} />
        </div>

        {/* Message Input */}
        <div className="border-t border-gray-200 bg-white">
          <MessageInput onSendMessage={handleSendMessage} disabled={!isConnected} />
        </div>
      </div>
    </div>
  );
};

export default ChatPage;
import React, { useState, useEffect } from 'react';
import { useWebSocket } from '../hooks/useWebSocket';
import { useAuth } from '../hooks/useAuth';
import { ChatMessage, MessageType } from '../types/chat';
import MessageList from '../components/MessageList';
import MessageInput from '../components/MessageInput';
import ConversationList from '../components/ConversationList';
import NetworkDebug from '../components/NetworkDebug';

const ChatPage: React.FC = () => {
  const { messages, sendMessage, isConnected } = useWebSocket();
  const { user, logout } = useAuth();
  const [selectedConversation, setSelectedConversation] = useState<string>('general');
  const [conversationMessages, setConversationMessages] = useState<ChatMessage[]>([]);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);

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

  const handleConversationSelect = (conversationId: string) => {
    setSelectedConversation(conversationId);
    setIsMobileSidebarOpen(false); // Close sidebar on mobile after selection
  };

  return (
    <div className="flex h-screen bg-white relative">
      {/* Mobile Sidebar Overlay */}
      {isMobileSidebarOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => setIsMobileSidebarOpen(false)}
        />
      )}

      {/* Sidebar - Conversations */}
      <div className={`
        fixed lg:relative lg:translate-x-0 z-50 lg:z-0
        w-80 lg:w-72 xl:w-80 bg-gray-50 border-r border-gray-200 h-full
        transition-transform duration-300 ease-in-out
        ${isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'}
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
                onClick={() => setIsMobileSidebarOpen(false)}
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
          selectedConversation={selectedConversation}
          onSelectConversation={handleConversationSelect}
        />
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col lg:ml-0">
        {/* Chat Header */}
        <div className="p-4 border-b border-gray-200 bg-white">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <button
                onClick={() => setIsMobileSidebarOpen(true)}
                className="lg:hidden p-2 -ml-2 text-gray-500 hover:text-gray-700 mr-2"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
              <div>
                <h2 className="text-lg font-medium text-gray-900">
                  {selectedConversation === 'general' ? 'General Chat' : 
                   selectedConversation === 'random' ? 'Random' :
                   selectedConversation === 'tech' ? 'Tech Talk' : selectedConversation}
                </h2>
                <div className="text-sm text-gray-500">
                  {conversationMessages.length} messages
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Messages */}
        <MessageList messages={conversationMessages} currentUserId={user?.id} />

        {/* Message Input */}
        <div className="border-t border-gray-200 bg-white">
          <MessageInput onSendMessage={handleSendMessage} disabled={!isConnected} />
        </div>
      </div>
      
      <NetworkDebug />
    </div>
  );
};

export default ChatPage;
import React, { useEffect, useRef } from 'react';
import { ChatMessage, MessageType } from '../types/chat';
import { format } from 'date-fns';

interface MessageListProps {
  messages: ChatMessage[];
  currentUserId?: string;
  isLoading?: boolean;
}

const MessageList: React.FC<MessageListProps> = ({ messages, currentUserId, isLoading = false }) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Debug logging
  useEffect(() => {
    console.log('MessageList render:', {
      currentUserId,
      messageCount: messages.length,
      sampleMessage: messages[0] ? {
        id: messages[0].id,
        senderId: messages[0].senderId,
        senderUsername: messages[0].senderUsername,
        isOwn: messages[0].senderId === currentUserId
      } : null
    });
  }, [messages, currentUserId]);

  return (
    <div className="flex-1 flex flex-col bg-gradient-to-b from-gray-50 to-gray-100">
      <div className="flex-1 overflow-y-auto p-2 sm:p-4 space-y-1">
        {isLoading && messages.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center space-y-4">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500"></div>
              <div className="text-gray-500 text-sm">Loading messages...</div>
            </div>
          </div>
        ) : messages.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="w-16 h-16 bg-gradient-to-br from-green-100 to-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <svg className="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <div className="text-gray-500 text-sm">No messages yet. Start the conversation!</div>
            </div>
          </div>
        ) : (
          <>
            {messages.map((message, index) => {
              const isOwn = message.senderId === currentUserId;
              console.log(`Message ${message.id}: senderId=${message.senderId}, currentUserId=${currentUserId}, isOwn=${isOwn}`);
              return (
                <div
                  key={message.id}
                  className="animate-fadeIn"
                  style={{ animationDelay: `${Math.min(index * 50, 500)}ms` }}
                >
                  <MessageBubble
                    message={message}
                    isOwn={isOwn}
                  />
                </div>
              );
            })}
          </>
        )}
        <div ref={messagesEndRef} />
      </div>
    </div>
  );
};

interface MessageBubbleProps {
  message: ChatMessage;
  isOwn: boolean;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isOwn }) => {
  const formatTime = (timestamp: string) => {
    const messageDate = new Date(timestamp);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const messageDay = new Date(messageDate.getFullYear(), messageDate.getMonth(), messageDate.getDate());
    
    if (messageDay.getTime() === today.getTime()) {
      return format(messageDate, 'HH:mm');
    } else if (messageDay.getTime() === today.getTime() - 24 * 60 * 60 * 1000) {
      return `Yesterday ${format(messageDate, 'HH:mm')}`;
    } else {
      return format(messageDate, 'MMM d, HH:mm');
    }
  };

  return (
    <div className={`flex mb-1 ${isOwn ? 'justify-end' : 'justify-start'} group`}>
      {/* Avatar for received messages */}
      {!isOwn && (
        <div className="flex-shrink-0 mr-2 self-end">
          <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center text-white text-xs font-semibold shadow-md">
            {(message.senderUsername || 'U').charAt(0).toUpperCase()}
          </div>
        </div>
      )}
      
      <div className={`relative max-w-[85%] sm:max-w-xs lg:max-w-md ${
        isOwn ? 'ml-12' : 'mr-12'
      }`}>
        {/* Message bubble */}
        <div className={`px-3 py-2 shadow-sm transition-all duration-200 group-hover:shadow-md ${
          isOwn 
            ? 'bg-gradient-to-r from-green-500 to-green-600 text-white rounded-2xl rounded-br-md' 
            : 'bg-white text-gray-900 rounded-2xl rounded-bl-md border border-gray-100'
        }`}>
          {/* Sender name for group chats */}
          {!isOwn && (
            <div className="text-xs font-semibold mb-1" style={{ color: `hsl(${message.senderUsername ? message.senderUsername.charCodeAt(0) * 7 % 360 : 0}, 70%, 45%)` }}>
              {message.senderUsername || 'Unknown User'}
            </div>
          )}
          
          {/* Message content */}
          <div className="text-sm leading-relaxed break-words whitespace-pre-wrap">
            {message.content}
          </div>
          
          {/* Timestamp and status */}
          <div className={`flex items-center justify-end mt-1 space-x-1 text-xs ${
            isOwn ? 'text-green-100' : 'text-gray-500'
          }`}>
            <span>{formatTime(message.timestamp)}</span>
            {isOwn && (
              <div className="flex items-center space-x-0.5">
                {/* Delivery status - double checkmark for delivered/read */}
                <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
                <svg className="w-3 h-3 -ml-1" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              </div>
            )}
          </div>
        </div>
        
        {/* Message tail */}
        <div className={`absolute bottom-0 w-0 h-0 ${
          isOwn 
            ? 'right-0 border-l-8 border-l-green-600 border-t-8 border-t-transparent' 
            : 'left-0 border-r-8 border-r-white border-t-8 border-t-transparent'
        }`}></div>
      </div>
    </div>
  );
};

export default MessageList;
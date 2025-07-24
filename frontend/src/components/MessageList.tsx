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
    <div className="flex-1 flex flex-col">
      <div className="flex-1 overflow-y-auto p-2 sm:p-4 space-y-2 sm:space-y-4">
        {isLoading && messages.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="text-gray-500 text-sm">Loading messages...</div>
          </div>
        ) : messages.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="text-gray-500 text-sm">No messages yet. Start the conversation!</div>
          </div>
        ) : (
          <>
            {messages.map((message) => {
              const isOwn = message.senderId === currentUserId;
              console.log(`Message ${message.id}: senderId=${message.senderId}, currentUserId=${currentUserId}, isOwn=${isOwn}`);
              return (
                <MessageBubble
                  key={message.id}
                  message={message}
                  isOwn={isOwn}
                />
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
  return (
    <div className={`flex ${isOwn ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[85%] sm:max-w-xs lg:max-w-md px-3 py-2 rounded-lg ${
        isOwn 
          ? 'bg-blue-500 text-white' 
          : 'bg-gray-100 text-gray-900'
      }`}>
        {!isOwn && (
          <div className="text-xs text-gray-600 mb-1 font-medium">
            {message.senderUsername || message.senderId || 'Unknown User'}
          </div>
        )}
        <div className="text-sm leading-relaxed break-words">{message.content}</div>
        <div className={`text-xs mt-1 ${
          isOwn ? 'text-blue-100' : 'text-gray-500'
        }`}>
          {format(new Date(message.timestamp), 'HH:mm')}
        </div>
      </div>
    </div>
  );
};

export default MessageList;
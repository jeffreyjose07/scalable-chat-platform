import React, { useEffect, useRef, memo, useCallback } from 'react';
import { ChatMessage, MessageType, MessageStatus } from '../types/chat';
import { format } from 'date-fns';

interface MessageListProps {
  messages: ChatMessage[];
  currentUserId?: string;
  isLoading?: boolean;
}

const MessageList: React.FC<MessageListProps> = memo(({ messages, currentUserId, isLoading = false }) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleJumpToMessage = useCallback((event: CustomEvent) => {
    const { messageId } = event.detail;
    console.log('🔍 Jump to message requested:', messageId);

    const messageElement = document.getElementById(`message-${messageId}`);
    if (messageElement) {
      console.log('✅ Found message element, scrolling to it');
      messageElement.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
      messageElement.classList.add('message-highlight');
      setTimeout(() => { messageElement.classList.remove('message-highlight'); }, 3000);
    } else {
      console.log('❌ Message element not found:', `message-${messageId}`);
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, []);

  useEffect(() => {
    const handleJumpEvent = (event: Event) => { handleJumpToMessage(event as CustomEvent); };
    window.addEventListener('jumpToMessage', handleJumpEvent);
    return () => { window.removeEventListener('jumpToMessage', handleJumpEvent); };
  }, [handleJumpToMessage]);

  useEffect(() => {
    console.log('MessageList render:', {
      currentUserId,
      messageCount: messages.length,
      sampleMessage: messages[0] ? {
        id: messages[0].id, senderId: messages[0].senderId,
        senderUsername: messages[0].senderUsername,
        isOwn: messages[0].senderId === currentUserId
      } : null
    });
  }, [messages, currentUserId]);

  return (
    <div className="h-full w-full">
      <div
        ref={messagesContainerRef}
        className="h-full w-full space-y-3"
        style={{ WebkitOverflowScrolling: 'touch', scrollBehavior: 'smooth' }}
      >
        {isLoading && messages.length === 0 ? (
          <div className="flex-1 flex items-center justify-center pt-20">
            <div className="flex flex-col items-center space-y-4">
              <div className="animate-spin rounded-full h-7 w-7 border-2 border-amber-700/30 border-t-amber-700"></div>
              <div className="text-gray-400 dark:text-zinc-500 text-sm font-medium">Loading messages…</div>
            </div>
          </div>
        ) : messages.length === 0 ? (
          <div className="flex-1 flex items-center justify-center pt-20">
            <div className="text-center">
              <div className="w-16 h-16 bg-amber-50 dark:bg-amber-900/20 rounded-2xl flex items-center justify-center mx-auto mb-4 border border-amber-100 dark:border-amber-800/30">
                <svg className="w-8 h-8 text-amber-600 dark:text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <div className="text-gray-500 dark:text-zinc-400 text-sm font-medium">No messages yet</div>
              <div className="text-gray-400 dark:text-zinc-500 text-xs mt-1">Start the conversation</div>
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
                  id={`message-${message.id}`}
                  className="animate-fadeIn message-container"
                  style={{ animationDelay: `${Math.min(index * 40, 400)}ms` }}
                >
                  <MessageBubble message={message} isOwn={isOwn} currentUserId={currentUserId} />
                </div>
              );
            })}
          </>
        )}
        <div ref={messagesEndRef} className="h-4" />
      </div>
    </div>
  );
});

interface MessageBubbleProps {
  message: ChatMessage;
  isOwn: boolean;
  currentUserId?: string;
}

const MessageBubble: React.FC<MessageBubbleProps> = memo(({ message, isOwn, currentUserId }) => {
  const getAvatarStyle = (username: string) => {
    const safe = String(username || 'U');
    const hue = safe.charCodeAt(0) * 7 % 360;
    return { backgroundColor: `hsl(${hue}, 65%, 50%)` };
  };

  const formatTime = (timestamp: string) => {
    const messageDate = new Date(timestamp);
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const messageDay = new Date(messageDate.getFullYear(), messageDate.getMonth(), messageDate.getDate());

    if (messageDay.getTime() === today.getTime()) {
      return format(messageDate, 'HH:mm');
    } else if (messageDay.getTime() === today.getTime() - 86400000) {
      return `Yesterday ${format(messageDate, 'HH:mm')}`;
    }
    return format(messageDate, 'MMM d, HH:mm');
  };

  return (
    <div className={`flex mb-1 ${isOwn ? 'justify-end' : 'justify-start'} group`}>
      {!isOwn && (
        <div className="flex-shrink-0 mr-2 self-end">
          <div
            className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-semibold shadow-sm"
            style={getAvatarStyle(message.senderUsername || 'U')}
          >
            {(message.senderUsername || 'U').charAt(0).toUpperCase()}
          </div>
        </div>
      )}

      <div className={`relative max-w-[80%] sm:max-w-xs lg:max-w-md ${isOwn ? 'ml-12' : 'mr-12'}`}>
        <div className={`px-4 py-2.5 shadow-sm transition-shadow duration-200 group-hover:shadow-md ${
          isOwn
            ? 'bg-amber-700 text-white rounded-2xl rounded-br-md'
            : 'bg-white dark:bg-zinc-800 text-gray-900 dark:text-gray-100 rounded-2xl rounded-bl-md border border-gray-100 dark:border-zinc-700'
        }`}>
          {!isOwn && (
            <div className="text-xs font-semibold mb-1 text-amber-700 dark:text-amber-400">
              {message.senderUsername || 'Unknown'}
            </div>
          )}

          <div className="text-sm leading-relaxed break-words whitespace-pre-wrap">
            {message.content}
          </div>

          <div className={`flex items-center justify-end mt-1 space-x-1 text-xs ${
            isOwn ? 'text-amber-100/80' : 'text-gray-400 dark:text-zinc-500'
          }`}>
            <span>{formatTime(message.timestamp)}</span>
            {isOwn && <MessageStatusIndicator message={message} currentUserId={currentUserId} />}
          </div>
        </div>
      </div>
    </div>
  );
});

interface MessageStatusIndicatorProps {
  message: ChatMessage;
  currentUserId?: string;
}

const MessageStatusIndicator: React.FC<MessageStatusIndicatorProps> = memo(({ message, currentUserId }) => {
  if (!currentUserId || message.senderId !== currentUserId) return null;

  return (
    <div className="flex items-center" title="Sent">
      <svg className="w-3 h-3 opacity-75" fill="currentColor" viewBox="0 0 20 20">
        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
      </svg>
    </div>
  );
});

export default MessageList;

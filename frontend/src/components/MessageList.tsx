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

  // Handle jump to message functionality
  const handleJumpToMessage = useCallback((event: CustomEvent) => {
    const { messageId } = event.detail;
    console.log('🔍 Jump to message requested:', messageId);
    
    const messageElement = document.getElementById(`message-${messageId}`);
    if (messageElement) {
      console.log('✅ Found message element, scrolling to it');
      
      // First scroll the message into view
      messageElement.scrollIntoView({ 
        behavior: 'smooth', 
        block: 'center',
        inline: 'nearest'
      });
      
      // Add a highlight effect to make the message stand out
      messageElement.classList.add('message-highlight');
      
      // Remove highlight after animation
      setTimeout(() => {
        messageElement.classList.remove('message-highlight');
      }, 3000);
    } else {
      console.log('❌ Message element not found:', `message-${messageId}`);
      // If message is not found, it might not be loaded yet
      // For now, we'll just scroll to the bottom
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, []);

  // Add event listener for jump to message
  useEffect(() => {
    const handleJumpEvent = (event: Event) => {
      handleJumpToMessage(event as CustomEvent);
    };
    
    window.addEventListener('jumpToMessage', handleJumpEvent);
    
    return () => {
      window.removeEventListener('jumpToMessage', handleJumpEvent);
    };
  }, [handleJumpToMessage]);

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
    <div className="h-full w-full">
      <div 
        ref={messagesContainerRef} 
        className="h-full w-full space-y-4"
        style={{
          // For iOS momentum scrolling
          WebkitOverflowScrolling: 'touch',
          // Smooth scrolling
          scrollBehavior: 'smooth',
        }}
      >
        {isLoading && messages.length === 0 ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="flex flex-col items-center space-y-4">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500"></div>
              <div className="text-gray-500 dark:text-gray-400 text-sm">Loading messages...</div>
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
              <div className="text-gray-500 dark:text-gray-400 text-sm">No messages yet. Start the conversation!</div>
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
                  style={{ animationDelay: `${Math.min(index * 50, 500)}ms` }}
                >
                  <MessageBubble
                    message={message}
                    isOwn={isOwn}
                    currentUserId={currentUserId}
                  />
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
  // Utility function to determine text color based on background brightness
  const getTextColorForBackground = (hue: number, saturation: number, lightness: number) => {
    // For light backgrounds (lightness > 60), use dark text
    // For dark backgrounds (lightness <= 60), use white text
    return lightness > 60 ? 'text-gray-800' : 'text-white';
  };

  // Generate consistent avatar colors for users
  const getAvatarStyle = (username: string) => {
    const safeUsername = String(username || 'U');
    const hue = safeUsername.charCodeAt(0) * 7 % 360;
    const saturation = 70;
    const lightness = 55;
    
    const backgroundColor = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
    const textColorClass = getTextColorForBackground(hue, saturation, lightness);
    
    return {
      backgroundColor,
      textColorClass
    };
  };
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
          {(() => {
            const avatarStyle = getAvatarStyle(message.senderUsername || 'U');
            return (
              <div 
                className={`w-8 h-8 rounded-full flex items-center justify-center ${avatarStyle.textColorClass} text-xs font-semibold shadow-md`}
                style={{ backgroundColor: avatarStyle.backgroundColor }}
              >
                {(message.senderUsername || 'U').charAt(0).toUpperCase()}
              </div>
            );
          })()}
        </div>
      )}
      
      <div className={`relative max-w-[85%] sm:max-w-xs lg:max-w-md ${
        isOwn ? 'ml-12' : 'mr-12'
      }`}>
        {/* Message bubble */}
        <div className={`px-4 py-3 shadow-sm transition-all duration-200 group-hover:shadow-md ${
          isOwn 
            ? 'bg-green-500 text-white rounded-2xl rounded-br-lg' 
            : 'bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded-2xl rounded-bl-lg'
        }`}>
          {/* Sender name for group chats */}
          {!isOwn && (
            <div className="text-xs font-semibold mb-1 text-gray-600 dark:text-gray-300">
              {message.senderUsername || 'Unknown User'}
            </div>
          )}
          
          {/* Message content */}
          <div className="text-sm leading-relaxed break-words whitespace-pre-wrap">
            {message.content}
          </div>
          
          {/* Timestamp and status */}
          <div className={`flex items-center justify-end mt-1 space-x-1 text-xs ${
            isOwn ? 'text-green-100' : 'text-gray-500 dark:text-gray-400'
          }`}>
            <span>{formatTime(message.timestamp)}</span>
            {isOwn && (
              <MessageStatusIndicator message={message} currentUserId={currentUserId} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
});

// Message status indicator component following WhatsApp standards
interface MessageStatusIndicatorProps {
  message: ChatMessage;
  currentUserId?: string;
}

const MessageStatusIndicator: React.FC<MessageStatusIndicatorProps> = memo(({ message, currentUserId }) => {
  // Simplified: just show "sent" status when message is saved in MongoDB
  // This means the message was successfully stored and will be picked up by receivers
  const showStatus = currentUserId && message.senderId === currentUserId;
  
  if (!showStatus) {
    return null; // Don't show status for received messages
  }
  
  // Simple "sent" indicator - message is saved in MongoDB and will be picked up by receivers
  return (
    <div className="flex items-center" title="Sent">
      <svg className="w-3 h-3 opacity-70" fill="currentColor" viewBox="0 0 20 20">
        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
      </svg>
    </div>
  );
});

export default MessageList;
import { useState, useEffect, useCallback, useRef } from 'react';
import { ChatMessage } from '../types/chat';

interface UseUnreadMessagesProps {
  messages: ChatMessage[];
  currentUserId?: string;
  selectedConversationId: string;
}

interface UseUnreadMessagesReturn {
  unreadCounts: Record<string, number>;
  markConversationAsRead: (conversationId: string) => void;
  getTotalUnreadCount: () => number;
  getUnreadCount: (conversationId: string) => number;
}

interface LastReadTimestamps {
  [conversationId: string]: string; // ISO timestamp
}

export const useUnreadMessages = ({
  messages,
  currentUserId,
  selectedConversationId
}: UseUnreadMessagesProps): UseUnreadMessagesReturn => {
  const [unreadCounts, setUnreadCounts] = useState<Record<string, number>>({});
  const [lastReadTimestamps, setLastReadTimestamps] = useState<LastReadTimestamps>({});
  const initialLoadCompleteRef = useRef(false);
  const markAsReadTimerRef = useRef<NodeJS.Timeout | null>(null);

  // Storage keys for localStorage
  const getLastReadKey = (userId: string) => `chat_last_read_${userId}`;

  // Load last read timestamps from localStorage
  const loadLastReadTimestamps = useCallback((userId: string): LastReadTimestamps => {
    try {
      const stored = localStorage.getItem(getLastReadKey(userId));
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (error) {
      console.warn('Failed to load last read timestamps from localStorage:', error);
    }
    return {};
  }, []);

  // Save last read timestamps to localStorage
  const saveLastReadTimestamps = useCallback((userId: string, timestamps: LastReadTimestamps) => {
    try {
      localStorage.setItem(getLastReadKey(userId), JSON.stringify(timestamps));
    } catch (error) {
      console.warn('Failed to save last read timestamps to localStorage:', error);
    }
  }, []);

  // Calculate unread count for a conversation using timestamp comparison
  const calculateUnreadCount = useCallback((conversationId: string): number => {
    if (!currentUserId) return 0;

    const lastReadAt = lastReadTimestamps[conversationId];
    const lastReadTimestamp = lastReadAt ? new Date(lastReadAt).getTime() : 0;

    return messages.filter(message => {
      // Skip messages from current user
      if (message.senderId === currentUserId) return false;
      
      // Only count messages in this conversation
      if (message.conversationId !== conversationId) return false;
      
      // Count message as unread if it's newer than last read timestamp
      const messageTimestamp = new Date(message.timestamp).getTime();
      return messageTimestamp > lastReadTimestamp;
    }).length;
  }, [messages, currentUserId, lastReadTimestamps]);

  // Initialize last read timestamps and calculate initial unread counts
  useEffect(() => {
    if (!initialLoadCompleteRef.current && currentUserId) {
      console.log('🚀 Initializing last read timestamps for user:', currentUserId);
      
      // Load saved timestamps from localStorage
      const savedTimestamps = loadLastReadTimestamps(currentUserId);
      setLastReadTimestamps(savedTimestamps);
      
      initialLoadCompleteRef.current = true;
      console.log('📚 Loaded last read timestamps:', savedTimestamps);
    }
  }, [currentUserId, loadLastReadTimestamps]);

  // Recalculate unread counts whenever messages or timestamps change
  useEffect(() => {
    if (!initialLoadCompleteRef.current || !currentUserId) return;

    const conversationIds = Array.from(new Set(messages.map(m => m.conversationId)));
    const newUnreadCounts: Record<string, number> = {};

    conversationIds.forEach(conversationId => {
      const count = calculateUnreadCount(conversationId);
      // Only include non-zero counts in the unread counts object
      if (count > 0) {
        newUnreadCounts[conversationId] = count;
      }
    });

    setUnreadCounts(newUnreadCounts);
    console.log('🔄 Recalculated unread counts:', newUnreadCounts);
  }, [messages, lastReadTimestamps, currentUserId, calculateUnreadCount, initialLoadCompleteRef.current]);

  // Handle conversation selection - immediate UI update, delayed persistence
  useEffect(() => {
    if (!selectedConversationId || !initialLoadCompleteRef.current || !currentUserId) return;

    // Immediately clear unread count in UI for better UX
    setUnreadCounts(prev => {
      const newCounts = { ...prev };
      // Remove the conversation from unread counts instead of setting to 0
      delete newCounts[selectedConversationId];
      return newCounts;
    });

    // Clear any existing timer
    if (markAsReadTimerRef.current) {
      clearTimeout(markAsReadTimerRef.current);
      markAsReadTimerRef.current = null;
    }

    // Set timer to mark as read after user has had time to see messages
    markAsReadTimerRef.current = setTimeout(() => {
      // Find the latest message timestamp in this conversation
      const conversationMessages = messages.filter(msg => 
        msg.conversationId === selectedConversationId
      );

      if (conversationMessages.length > 0) {
        // Get the latest message timestamp
        const latestTimestamp = conversationMessages
          .map(msg => new Date(msg.timestamp).getTime())
          .reduce((latest, current) => Math.max(latest, current), 0);

        const newTimestamps = {
          ...lastReadTimestamps,
          [selectedConversationId]: new Date(latestTimestamp).toISOString()
        };

        setLastReadTimestamps(newTimestamps);
        saveLastReadTimestamps(currentUserId, newTimestamps);
        
        console.log('✅ Updated last read timestamp for:', selectedConversationId, 'to:', new Date(latestTimestamp).toISOString());
      }

      markAsReadTimerRef.current = null;
    }, 1500); // 1.5 seconds - reasonable time to see messages

    // Cleanup function
    return () => {
      if (markAsReadTimerRef.current) {
        clearTimeout(markAsReadTimerRef.current);
        markAsReadTimerRef.current = null;
      }
    };
  }, [selectedConversationId, messages, currentUserId, lastReadTimestamps, saveLastReadTimestamps]);

  // Manual mark as read function
  const markConversationAsRead = useCallback((conversationId: string) => {
    if (!currentUserId) return;

    // Clear unread count immediately
    setUnreadCounts(prev => ({
      ...prev,
      [conversationId]: 0
    }));

    // Find the latest message timestamp in this conversation
    const conversationMessages = messages.filter(msg => 
      msg.conversationId === conversationId
    );

    if (conversationMessages.length > 0) {
      const latestTimestamp = conversationMessages
        .map(msg => new Date(msg.timestamp).getTime())
        .reduce((latest, current) => Math.max(latest, current), 0);

      const newTimestamps = {
        ...lastReadTimestamps,
        [conversationId]: new Date(latestTimestamp).toISOString()
      };

      setLastReadTimestamps(newTimestamps);
      saveLastReadTimestamps(currentUserId, newTimestamps);
      
      console.log('💾 Manually marked conversation as read:', conversationId);
    }
  }, [currentUserId, messages, lastReadTimestamps, saveLastReadTimestamps]);

  const getTotalUnreadCount = useCallback(() => {
    return Object.values(unreadCounts).reduce((total, count) => total + count, 0);
  }, [unreadCounts]);

  const getUnreadCount = useCallback((conversationId: string) => {
    return unreadCounts[conversationId] || 0;
  }, [unreadCounts]);

  // Cleanup on user change (logout/login)
  useEffect(() => {
    return () => {
      if (markAsReadTimerRef.current) {
        clearTimeout(markAsReadTimerRef.current);
        markAsReadTimerRef.current = null;
      }
      
      // Reset state for new user
      setUnreadCounts({});
      setLastReadTimestamps({});
      initialLoadCompleteRef.current = false;
    };
  }, [currentUserId]);

  return {
    unreadCounts,
    markConversationAsRead,
    getTotalUnreadCount,
    getUnreadCount
  };
};
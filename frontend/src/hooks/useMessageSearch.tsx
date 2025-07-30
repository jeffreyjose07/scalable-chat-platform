import { useState, useCallback } from 'react';
import { api, SearchResult } from '../services/api';
import { SearchFilters } from '../components/NewMessageSearchBar';

interface UseMessageSearchReturn {
  isSearchMode: boolean;
  searchResult: SearchResult | null;
  isSearchLoading: boolean;
  searchError: string | null;
  toggleSearchMode: () => void;
  performSearch: (conversationId: string, query: string, filters?: SearchFilters) => Promise<void>;
  clearSearch: () => void;
  loadMoreResults: (conversationId: string) => Promise<void>;
  jumpToMessage: (messageId: string) => void;
}

export const useMessageSearch = (): UseMessageSearchReturn => {
  const [isSearchMode, setIsSearchMode] = useState(false);
  const [searchResult, setSearchResult] = useState<SearchResult | null>(null);
  const [isSearchLoading, setIsSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const toggleSearchMode = useCallback(() => {
    setIsSearchMode(prev => {
      if (prev) {
        // Clear search when closing
        setSearchResult(null);
        setSearchError(null);
      }
      return !prev;
    });
  }, []);

  const performSearch = useCallback(async (conversationId: string, query: string, filters?: SearchFilters) => {
    console.log('🔍 performSearch called:', { conversationId, query, filters });
    
    if (!query.trim()) {
      console.log('❌ Empty query, clearing search result');
      setSearchResult(null);
      return;
    }

    if (!conversationId) {
      console.log('❌ No conversation selected, cannot search');
      setSearchError('Please select a conversation to search');
      return;
    }

    setIsSearchLoading(true);
    setSearchError(null);
    
    try {
      console.log('🔄 Calling API search for conversation:', conversationId, 'query:', query);
      // For now, we'll just use the basic search API
      // TODO: Extend backend API to support filters
      const result = await api.messageSearch.searchMessages(conversationId, query);
      console.log('✅ Search API returned:', result.totalCount, 'results');
      
      // Client-side filtering as fallback until backend supports filters
      if (filters && result.messages) {
        let filteredMessages = result.messages;
        
        if (filters.sender) {
          filteredMessages = filteredMessages.filter(msg => 
            msg.senderUsername.toLowerCase().includes(filters.sender!.toLowerCase())
          );
        }
        
        if (filters.dateFrom) {
          const fromDate = new Date(filters.dateFrom);
          filteredMessages = filteredMessages.filter(msg => 
            new Date(msg.timestamp) >= fromDate
          );
        }
        
        if (filters.dateTo) {
          const toDate = new Date(filters.dateTo);
          toDate.setHours(23, 59, 59, 999); // End of day
          filteredMessages = filteredMessages.filter(msg => 
            new Date(msg.timestamp) <= toDate
          );
        }
        
        // Note: hasMedia filter would require backend support
        // as message content field doesn't contain attachment info
        
        result.messages = filteredMessages;
        result.totalCount = filteredMessages.length;
      }
      
      setSearchResult(result);
    } catch (error) {
      console.error('Failed to search messages:', error);
      setSearchError('Failed to search messages. Please try again.');
      setSearchResult(null);
    } finally {
      setIsSearchLoading(false);
    }
  }, []);

  const clearSearch = useCallback(() => {
    setSearchResult(null);
    setSearchError(null);
  }, []);

  const loadMoreResults = useCallback(async (conversationId: string) => {
    if (!searchResult || !searchResult.hasMore) return;

    setIsSearchLoading(true);
    try {
      const nextPage = await api.messageSearch.searchMessages(
        conversationId,
        searchResult.query,
        searchResult.currentPage + 1,
        20 // page size
      );
      
      // Append new results to existing ones
      setSearchResult(prev => prev ? {
        ...nextPage,
        messages: [...prev.messages, ...nextPage.messages]
      } : nextPage);
    } catch (error) {
      console.error('Failed to load more results:', error);
      setSearchError('Failed to load more results');
    } finally {
      setIsSearchLoading(false);
    }
  }, [searchResult]);

  const jumpToMessage = useCallback((messageId: string) => {
    // TODO: Implement message jumping/scrolling logic
    console.log('Jump to message:', messageId);
    
    // Emit custom event that MessageList can listen to
    window.dispatchEvent(new CustomEvent('jumpToMessage', { 
      detail: { messageId } 
    }));
    
    // Close search mode after a brief delay to allow jumping
    setTimeout(() => {
      setIsSearchMode(false);
      setSearchResult(null);
    }, 100);
  }, []);

  return {
    isSearchMode,
    searchResult,
    isSearchLoading,
    searchError,
    toggleSearchMode,
    performSearch,
    clearSearch,
    loadMoreResults,
    jumpToMessage
  };
};
import { useState, useCallback } from 'react';
import { api, SearchResult } from '../services/api';

interface UseMessageSearchReturn {
  isSearchMode: boolean;
  searchResult: SearchResult | null;
  isSearchLoading: boolean;
  searchError: string | null;
  toggleSearchMode: () => void;
  performSearch: (conversationId: string, query: string) => Promise<void>;
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

  const performSearch = useCallback(async (conversationId: string, query: string) => {
    if (!query.trim()) {
      setSearchResult(null);
      return;
    }

    setIsSearchLoading(true);
    setSearchError(null);
    
    try {
      const result = await api.messageSearch.searchMessages(conversationId, query);
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
        searchResult.currentPage + 1
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
    
    // For now, just close search mode
    setIsSearchMode(false);
    setSearchResult(null);
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
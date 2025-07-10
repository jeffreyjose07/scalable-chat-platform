import React, { useState, useRef, useEffect } from 'react';

interface MessageSearchBarProps {
  isSearchMode: boolean;
  onToggleSearch: () => void;
  onSearch: (query: string) => void;
  onClearSearch: () => void;
  isLoading?: boolean;
  resultsCount?: number;
  placeholder?: string;
}

const MessageSearchBar: React.FC<MessageSearchBarProps> = ({
  isSearchMode,
  onToggleSearch,
  onSearch,
  onClearSearch,
  isLoading = false,
  resultsCount,
  placeholder = "Search messages..."
}) => {
  const [query, setQuery] = useState('');
  const searchInputRef = useRef<HTMLInputElement>(null);
  const searchTimeoutRef = useRef<NodeJS.Timeout>();

  // Focus input when search mode is activated
  useEffect(() => {
    if (isSearchMode && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isSearchMode]);

  // Clear query when search mode is closed
  useEffect(() => {
    if (!isSearchMode) {
      setQuery('');
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    }
  }, [isSearchMode]);

  // Debounced search
  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    if (isSearchMode && query.trim()) {
      searchTimeoutRef.current = setTimeout(() => {
        onSearch(query.trim());
      }, 300);
    } else if (isSearchMode && query === '') {
      onClearSearch();
    }

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [query, isSearchMode, onSearch, onClearSearch]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      handleClose();
    }
  };

  const handleClose = () => {
    setQuery('');
    onClearSearch();
    onToggleSearch();
  };

  const handleClear = () => {
    setQuery('');
    onClearSearch();
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  };

  if (!isSearchMode) {
    // Search toggle button
    return (
      <button
        onClick={onToggleSearch}
        className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
        title="Search messages"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </button>
    );
  }

  return (
    <div className="flex-1 max-w-md">
      <div className="relative">
        {/* Search input */}
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            {isLoading ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-400"></div>
            ) : (
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            )}
          </div>
          
          <input
            ref={searchInputRef}
            type="text"
            placeholder={placeholder}
            value={query}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            className="w-full pl-10 pr-20 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
          />
          
          <div className="absolute inset-y-0 right-0 flex items-center">
            {/* Results count */}
            {resultsCount !== undefined && query && (
              <span className="text-xs text-gray-500 mr-2">
                {resultsCount} {resultsCount === 1 ? 'result' : 'results'}
              </span>
            )}
            
            {/* Clear button */}
            {query && (
              <button
                onClick={handleClear}
                className="p-1 text-gray-400 hover:text-gray-600 transition-colors mr-1"
                title="Clear search"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
            
            {/* Close button */}
            <button
              onClick={handleClose}
              className="p-1 text-gray-400 hover:text-gray-600 transition-colors mr-2"
              title="Close search"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        
        {/* Search tips */}
        {isSearchMode && !query && (
          <div className="absolute top-full left-0 right-0 mt-1 p-2 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-600 z-10">
            <div className="flex items-center space-x-4">
              <span>💡 Search for messages in this conversation</span>
              <span className="text-gray-400">Press Esc to close</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default MessageSearchBar;
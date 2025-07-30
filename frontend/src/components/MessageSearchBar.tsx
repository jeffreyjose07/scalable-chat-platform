import React, { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';

export interface SearchFilters {
  sender?: string;
  dateFrom?: string;
  dateTo?: string;
  hasMedia?: boolean;
}

interface MessageSearchBarProps {
  isSearchMode: boolean;
  onToggleSearch: () => void;
  onSearch: (query: string, filters?: SearchFilters) => void;
  onClearSearch: () => void;
  isLoading?: boolean;
  resultsCount?: number;
  placeholder?: string;
  enableFilters?: boolean;
}

const MessageSearchBar: React.FC<MessageSearchBarProps> = ({
  isSearchMode,
  onToggleSearch,
  onSearch,
  onClearSearch,
  isLoading = false,
  resultsCount,
  placeholder = "Search messages...",
  enableFilters = true
}) => {
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState<SearchFilters>({});
  const [showFilters, setShowFilters] = useState(false);
  const [recentSearches, setRecentSearches] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [dropdownPosition, setDropdownPosition] = useState<{ top: number; left: number; width: number } | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const searchContainerRef = useRef<HTMLDivElement>(null);
  const searchTimeoutRef = useRef<NodeJS.Timeout>();
  const filtersRef = useRef<HTMLDivElement>(null);

  // Load recent searches from localStorage
  useEffect(() => {
    const stored = localStorage.getItem('recentSearches');
    if (stored) {
      try {
        setRecentSearches(JSON.parse(stored));
      } catch (e) {
        console.warn('Failed to parse recent searches');
      }
    }
  }, []);

  // Focus input when search mode is activated
  useEffect(() => {
    if (isSearchMode && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isSearchMode]);

  // Handle clicks outside filters panel and suggestions dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node;
      
      // Close filters panel if clicked outside
      if (filtersRef.current && !filtersRef.current.contains(target)) {
        setShowFilters(false);
      }
      
      // Close suggestions dropdown if clicked outside
      if (showSuggestions && searchContainerRef.current && !searchContainerRef.current.contains(target)) {
        setShowSuggestions(false);
        setDropdownPosition(null);
      }
    };

    if (showFilters || showSuggestions) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showFilters, showSuggestions]);

  // Clear query when search mode is closed
  useEffect(() => {
    if (!isSearchMode) {
      setQuery('');
      setShowSuggestions(false);
      setDropdownPosition(null);
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    }
  }, [isSearchMode]);

  // Calculate dropdown position
  const calculateDropdownPosition = () => {
    if (!searchContainerRef.current) return null;
    
    const rect = searchContainerRef.current.getBoundingClientRect();
    return {
      top: rect.bottom + window.scrollY + 4, // 4px gap (mt-1)
      left: rect.left + window.scrollX,
      width: rect.width
    };
  };

  // Debounced search
  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    if (isSearchMode && query.trim()) {
      searchTimeoutRef.current = setTimeout(() => {
        performSearch(query.trim());
      }, 300);
    } else if (isSearchMode && query === '') {
      onClearSearch();
    }

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [query, filters, isSearchMode]);

  const performSearch = (searchQuery: string) => {
    // Save to recent searches
    const newRecentSearches = [searchQuery, ...recentSearches.filter(s => s !== searchQuery)].slice(0, 5);
    setRecentSearches(newRecentSearches);
    localStorage.setItem('recentSearches', JSON.stringify(newRecentSearches));
    
    setShowSuggestions(false);
    onSearch(searchQuery, Object.keys(filters).length > 0 ? filters : undefined);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      if (showSuggestions) {
        setShowSuggestions(false);
        setDropdownPosition(null);
      } else {
        handleClose();
      }
    } else if (e.key === 'Enter' && query.trim()) {
      e.preventDefault();
      performSearch(query.trim());
      setShowSuggestions(false);
      setDropdownPosition(null);
    } else if (e.key === 'ArrowDown' && showSuggestions) {
      e.preventDefault();
      // TODO: Implement arrow key navigation for suggestions
    }
  };

  const handleClose = () => {
    setQuery('');
    onClearSearch();
    onToggleSearch();
  };

  const handleClear = () => {
    setQuery('');
    setFilters({});
    setShowSuggestions(false);
    onClearSearch();
    if (searchInputRef.current) {
      searchInputRef.current.focus();
    }
  };

  const handleFilterChange = (key: keyof SearchFilters, value: any) => {
    const newFilters = { ...filters };
    if (value === '' || value === undefined || value === false) {
      delete newFilters[key];
    } else {
      newFilters[key] = value;
    }
    setFilters(newFilters);
  };

  const clearFilters = () => {
    setFilters({});
    // If there's a current query, re-search without filters
    if (query.trim()) {
      performSearch(query.trim());
    }
  };

  const hasActiveFilters = Object.keys(filters).length > 0;

  if (!isSearchMode) {
    // Search toggle button
    return (
      <button
        onClick={onToggleSearch}
        className="p-2 text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
        title="Search messages"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </button>
    );
  }

  return (
    <div className="flex-1 w-full lg:max-w-md">
      <div className="relative" ref={searchContainerRef}>
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
            onFocus={() => {
              if (recentSearches.length > 0 && !query) {
                const position = calculateDropdownPosition();
                if (position) {
                  setDropdownPosition(position);
                  setShowSuggestions(true);
                }
              }
            }}
            className="w-full pl-10 pr-32 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
          />
          
          <div className="absolute inset-y-0 right-0 flex items-center">
            {/* Results count */}
            {resultsCount !== undefined && query && (
              <span className="text-xs text-gray-500 dark:text-gray-400 mr-2">
                {resultsCount} {resultsCount === 1 ? 'result' : 'results'}
              </span>
            )}
            
            {/* Filter toggle button */}
            {enableFilters && (
              <button
                onClick={() => setShowFilters(!showFilters)}
                className={`p-1 transition-colors mr-1 ${
                  hasActiveFilters
                    ? 'text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300'
                    : 'text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300'
                }`}
                title="Search filters"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.707A1 1 0 013 7V4z" />
                </svg>
                {hasActiveFilters && (
                  <div className="absolute -top-1 -right-1 w-2 h-2 bg-blue-600 rounded-full"></div>
                )}
              </button>
            )}
            
            {/* Clear button */}
            {(query || hasActiveFilters) && (
              <button
                onClick={handleClear}
                className="p-1 text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 transition-colors mr-1"
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
              className="p-1 text-gray-400 dark:text-gray-500 hover:text-gray-600 dark:hover:text-gray-300 transition-colors mr-2"
              title="Close search"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
        
        {/* Search suggestions - rendered via Portal */}
        {showSuggestions && recentSearches.length > 0 && dropdownPosition && createPortal(
          <div 
            className="fixed bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg z-[9999]"
            style={{
              top: `${dropdownPosition.top}px`,
              left: `${dropdownPosition.left}px`,
              width: `${dropdownPosition.width}px`
            }}
          >
            <div className="p-2 border-b border-gray-100 dark:border-gray-700">
              <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Recent searches</span>
            </div>
            {recentSearches.map((search, index) => (
              <button
                key={index}
                onClick={(e: React.MouseEvent<HTMLButtonElement>) => {
                  e.preventDefault();
                  e.stopPropagation();
                  
                  console.log('Recent search clicked:', search);
                  
                  // Clear any pending timeouts
                  if (searchTimeoutRef.current) {
                    clearTimeout(searchTimeoutRef.current);
                  }
                  
                  // Hide suggestions immediately
                  setShowSuggestions(false);
                  setDropdownPosition(null);
                  
                  // Update query state immediately
                  setQuery(search);
                  if (searchInputRef.current) {
                    searchInputRef.current.value = search;
                    searchInputRef.current.focus();
                  }
                  
                  // Save to recent searches
                  const newRecentSearches = [search, ...recentSearches.filter(s => s !== search)].slice(0, 5);
                  setRecentSearches(newRecentSearches);
                  localStorage.setItem('recentSearches', JSON.stringify(newRecentSearches));
                  
                  // Directly call onSearch to ensure it works immediately
                  console.log('🔍 Recent search: calling onSearch with:', search);
                  onSearch(search, Object.keys(filters).length > 0 ? filters : undefined);
                }}
                className="w-full text-left px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-600 text-sm flex items-center space-x-2"
              >
                <svg className="w-4 h-4 text-gray-400 dark:text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span className="flex-1 text-gray-900 dark:text-gray-100">{search}</span>
              </button>
            ))}
          </div>,
          document.body
        )}

        {/* Search tips */}
        {isSearchMode && !query && !showSuggestions && (
          <div className="absolute top-full left-0 right-0 mt-1 p-2 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg text-xs text-gray-600 dark:text-gray-300 z-[9999]">
            <div className="flex items-center space-x-4">
              <span>💡 Search for messages in this conversation</span>
              <span className="text-gray-400 dark:text-gray-500">Press Esc to close</span>
            </div>
          </div>
        )}

        {/* Advanced filters */}
        {showFilters && enableFilters && (
          <div ref={filtersRef} className="absolute top-full left-0 right-0 mt-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-xl z-[9999] p-4 max-w-sm lg:max-w-md">
            <div className="flex items-center justify-between mb-3">
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Search Filters</span>
              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  className="text-xs text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300"
                >
                  Clear all
                </button>
              )}
            </div>
            
            <div className="space-y-3">
              {/* Sender filter */}
              <div>
                <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">From user</label>
                <input
                  type="text"
                  placeholder="Username..."
                  value={filters.sender || ''}
                  onChange={(e) => handleFilterChange('sender', e.target.value)}
                  className="w-full px-2 py-1 text-xs border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 rounded focus:ring-1 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              
              {/* Date range */}
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">From date</label>
                  <input
                    type="date"
                    value={filters.dateFrom || ''}
                    onChange={(e) => handleFilterChange('dateFrom', e.target.value)}
                    className="w-full px-2 py-1 text-xs border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 rounded focus:ring-1 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">To date</label>
                  <input
                    type="date"
                    value={filters.dateTo || ''}
                    onChange={(e) => handleFilterChange('dateTo', e.target.value)}
                    className="w-full px-2 py-1 text-xs border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 placeholder-gray-500 dark:placeholder-gray-400 rounded focus:ring-1 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>
              
              {/* Media filter */}
              <div>
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={filters.hasMedia || false}
                    onChange={(e) => handleFilterChange('hasMedia', e.target.checked)}
                    className="w-3 h-3 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  />
                  <span className="text-xs text-gray-600 dark:text-gray-400">Has attachments</span>
                </label>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default MessageSearchBar;
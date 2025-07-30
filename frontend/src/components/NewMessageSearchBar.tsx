import React, { useState, useRef, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';

export interface SearchFilters {
  sender?: string;
  dateFrom?: string;
  dateTo?: string;
  hasMedia?: boolean;
}

interface NewMessageSearchBarProps {
  isSearchMode: boolean;
  onToggleSearch: () => void;
  onSearch: (query: string, filters?: SearchFilters) => void;
  onClearSearch: () => void;
  isLoading?: boolean;
  resultsCount?: number;
  placeholder?: string;
  enableFilters?: boolean;
}

const NewMessageSearchBar: React.FC<NewMessageSearchBarProps> = ({
  isSearchMode,
  onToggleSearch,
  onSearch,
  onClearSearch,
  isLoading = false,
  resultsCount,
  placeholder = "Search messages...",
  enableFilters = true
}) => {
  // Core state
  const [query, setQuery] = useState('');
  const [filters, setFilters] = useState<SearchFilters>({});
  const [showFilters, setShowFilters] = useState(false);
  
  // Recent searches state
  const [recentSearches, setRecentSearches] = useState<string[]>([]);
  const [showRecentDropdown, setShowRecentDropdown] = useState(false);
  const [dropdownPosition, setDropdownPosition] = useState<{ top: number; left: number; width: number } | null>(null);
  
  // Refs
  const searchInputRef = useRef<HTMLInputElement>(null);
  const searchContainerRef = useRef<HTMLDivElement>(null);
  const filtersRef = useRef<HTMLDivElement>(null);
  const debounceTimeoutRef = useRef<NodeJS.Timeout>();
  
  // Load recent searches from localStorage on mount
  useEffect(() => {
    try {
      const stored = localStorage.getItem('recentSearches');
      if (stored) {
        const parsed = JSON.parse(stored);
        setRecentSearches(Array.isArray(parsed) ? parsed : []);
      }
    } catch (error) {
      console.warn('Failed to load recent searches:', error);
      setRecentSearches([]);
    }
  }, []);

  // Focus input when search mode is activated
  useEffect(() => {
    if (isSearchMode && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isSearchMode]);

  // Clear state when search mode is closed
  useEffect(() => {
    if (!isSearchMode) {
      setQuery('');
      setShowRecentDropdown(false);
      setDropdownPosition(null);
      setShowFilters(false);
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    }
  }, [isSearchMode]);

  // Handle clicks outside to close dropdowns
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node;
      
      // Close filters panel if clicked outside
      if (filtersRef.current && !filtersRef.current.contains(target)) {
        setShowFilters(false);
      }
      
      // Close recent searches dropdown if clicked outside
      if (showRecentDropdown && searchContainerRef.current && !searchContainerRef.current.contains(target)) {
        setShowRecentDropdown(false);
        setDropdownPosition(null);
      }
    };

    if (showFilters || showRecentDropdown) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showFilters, showRecentDropdown]);

  // Calculate dropdown position
  const calculateDropdownPosition = useCallback(() => {
    if (!searchContainerRef.current) return null;
    
    const rect = searchContainerRef.current.getBoundingClientRect();
    return {
      top: rect.bottom + window.scrollY + 4,
      left: rect.left + window.scrollX,
      width: rect.width
    };
  }, []);

  // Save search to recent searches
  const saveToRecentSearches = useCallback((searchQuery: string) => {
    const trimmed = searchQuery.trim();
    if (!trimmed) return;
    
    const newRecentSearches = [trimmed, ...recentSearches.filter(s => s !== trimmed)].slice(0, 5);
    setRecentSearches(newRecentSearches);
    
    try {
      localStorage.setItem('recentSearches', JSON.stringify(newRecentSearches));
    } catch (error) {
      console.warn('Failed to save recent searches:', error);
    }
  }, [recentSearches]);

  // Execute search function
  const executeSearch = useCallback((searchQuery: string, searchFilters?: SearchFilters) => {
    const trimmed = searchQuery.trim();
    if (!trimmed) {
      onClearSearch();
      return;
    }
    
    console.log('🔍 NewMessageSearchBar executing search:', { query: trimmed, filters: searchFilters });
    
    // Save to recent searches
    saveToRecentSearches(trimmed);
    
    // Hide dropdown
    setShowRecentDropdown(false);
    setDropdownPosition(null);
    
    // Execute search
    onSearch(trimmed, searchFilters);
  }, [onSearch, onClearSearch, saveToRecentSearches]);

  // Handle input change with debouncing
  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    
    // Clear existing timeout
    if (debounceTimeoutRef.current) {
      clearTimeout(debounceTimeoutRef.current);
    }
    
    // If input is empty, clear search immediately
    if (!value.trim()) {
      onClearSearch();
      return;
    }
    
    // Debounce search execution
    debounceTimeoutRef.current = setTimeout(() => {
      executeSearch(value, Object.keys(filters).length > 0 ? filters : undefined);
    }, 300);
  }, [executeSearch, filters, onClearSearch]);

  // Handle input focus
  const handleInputFocus = useCallback(() => {
    if (recentSearches.length > 0 && !query.trim()) {
      const position = calculateDropdownPosition();
      if (position) {
        setDropdownPosition(position);
        setShowRecentDropdown(true);
      }
    }
  }, [recentSearches.length, query, calculateDropdownPosition]);

  // Handle recent search selection
  const handleRecentSearchSelect = useCallback((searchQuery: string) => {
    console.log('🔍 Recent search selected:', searchQuery);
    
    // Update input
    setQuery(searchQuery);
    if (searchInputRef.current) {
      searchInputRef.current.value = searchQuery;
    }
    
    // Execute search immediately
    executeSearch(searchQuery, Object.keys(filters).length > 0 ? filters : undefined);
  }, [executeSearch, filters]);

  // Handle key events
  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      if (showRecentDropdown) {
        setShowRecentDropdown(false);
        setDropdownPosition(null);
      } else {
        onToggleSearch();
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      executeSearch(query, Object.keys(filters).length > 0 ? filters : undefined);
    }
  }, [showRecentDropdown, onToggleSearch, executeSearch, query, filters]);

  // Handle clear
  const handleClear = useCallback(() => {
    setQuery('');
    setFilters({});
    if (searchInputRef.current) {
      searchInputRef.current.value = '';
      searchInputRef.current.focus();
    }
    onClearSearch();
  }, [onClearSearch]);

  // Handle close
  const handleClose = useCallback(() => {
    setQuery('');
    setFilters({});
    setShowRecentDropdown(false);
    setDropdownPosition(null);
    onToggleSearch();
  }, [onToggleSearch]);

  // Filter utilities
  const hasActiveFilters = Object.values(filters).some(value => 
    value !== undefined && value !== '' && value !== false
  );

  const handleFilterChange = useCallback((filterKey: keyof SearchFilters, value: any) => {
    const newFilters = { ...filters };
    if (value === undefined || value === '' || value === false) {
      delete newFilters[filterKey];
    } else {
      newFilters[filterKey] = value;
    }
    setFilters(newFilters);
    
    // Re-execute search if there's a query
    if (query.trim()) {
      executeSearch(query, Object.keys(newFilters).length > 0 ? newFilters : undefined);
    }
  }, [filters, query, executeSearch]);

  // If not in search mode, show toggle button
  if (!isSearchMode) {
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
            onFocus={handleInputFocus}
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

        {/* Filters panel */}
        {showFilters && (
          <div
            ref={filtersRef}
            className="absolute top-full left-0 right-0 mt-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg z-50 p-4"
          >
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                  From User
                </label>
                <input
                  type="text"
                  placeholder="Username"
                  value={filters.sender || ''}
                  onChange={(e) => handleFilterChange('sender', e.target.value)}
                  className="w-full px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded focus:ring-1 focus:ring-blue-500"
                />
              </div>
              
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                  From Date
                </label>
                <input
                  type="date"
                  value={filters.dateFrom || ''}
                  onChange={(e) => handleFilterChange('dateFrom', e.target.value)}
                  className="w-full px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded focus:ring-1 focus:ring-blue-500"
                />
              </div>
              
              <div>
                <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">
                  To Date
                </label>
                <input
                  type="date"
                  value={filters.dateTo || ''}
                  onChange={(e) => handleFilterChange('dateTo', e.target.value)}
                  className="w-full px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100 rounded focus:ring-1 focus:ring-blue-500"
                />
              </div>
              
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="hasMedia"
                  checked={filters.hasMedia || false}
                  onChange={(e) => handleFilterChange('hasMedia', e.target.checked)}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 dark:border-gray-600 rounded"
                />
                <label htmlFor="hasMedia" className="ml-2 text-xs font-medium text-gray-700 dark:text-gray-300">
                  Has Media
                </label>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Recent searches dropdown */}
      {showRecentDropdown && dropdownPosition && recentSearches.length > 0 && createPortal(
        <div
          className="absolute bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg z-50 max-h-48 overflow-y-auto"
          style={{
            top: dropdownPosition.top,
            left: dropdownPosition.left,
            width: dropdownPosition.width
          }}
        >
          <div className="p-2 border-b border-gray-100 dark:border-gray-700">
            <span className="text-xs font-medium text-gray-500 dark:text-gray-400">Recent searches</span>
          </div>
          {recentSearches.map((search, index) => (
            <button
              key={index}
              onClick={() => handleRecentSearchSelect(search)}
              className="w-full text-left px-3 py-2 hover:bg-gray-50 dark:hover:bg-gray-600 text-sm flex items-center space-x-2 transition-colors"
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
    </div>
  );
};

export default NewMessageSearchBar;
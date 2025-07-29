import React, { useState } from 'react';
import { MessageSearchResult, SearchResult } from '../services/api';

interface SearchResultsListProps {
  searchResult: SearchResult | null;
  isLoading: boolean;
  error: string | null;
  onJumpToMessage: (messageId: string) => void;
  onLoadMore?: () => void;
  onShowContext?: (messageId: string) => void;
  className?: string;
}

const SearchResultsList: React.FC<SearchResultsListProps> = ({
  searchResult,
  isLoading,
  error,
  onJumpToMessage,
  onLoadMore,
  onShowContext,
  className = ''
}) => {
  const [selectedResult, setSelectedResult] = useState<string | null>(null);
  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);
    
    if (diffInHours < 24) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffInHours < 168) { // 7 days
      return date.toLocaleDateString([], { weekday: 'short', hour: '2-digit', minute: '2-digit' });
    } else {
      return date.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }
  };

  const renderHighlightedContent = (result: MessageSearchResult) => {
    if (result.highlightedContent) {
      return (
        <div 
          className="text-sm text-gray-700 leading-relaxed"
          dangerouslySetInnerHTML={{ 
            __html: result.highlightedContent.replace(
              /<mark>/g, 
              '<mark class="bg-yellow-300 text-yellow-900 px-1 py-0.5 rounded font-medium shadow-sm">'
            )
          }}
        />
      );
    }
    
    return <div className="text-sm text-gray-700 leading-relaxed">{result.content}</div>;
  };

  const formatRelativeTime = (timestamp: string) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);
    const diffInDays = Math.floor(diffInHours / 24);
    
    if (diffInHours < 1) {
      return 'Just now';
    } else if (diffInHours < 24) {
      return `${Math.floor(diffInHours)}h ago`;
    } else if (diffInDays === 1) {
      return 'Yesterday';
    } else if (diffInDays < 7) {
      return `${diffInDays} days ago`;
    } else {
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    }
  };

  if (error) {
    return (
      <div className={`p-4 ${className}`}>
        <div className="text-center py-8">
          <svg className="w-12 h-12 text-red-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h3 className="text-lg font-medium text-gray-900 mb-2">Search Error</h3>
          <p className="text-gray-500">{error}</p>
        </div>
      </div>
    );
  }

  if (isLoading && !searchResult) {
    return (
      <div className={`p-4 ${className}`}>
        <div className="text-center py-8">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mb-4"></div>
          <p className="text-gray-600">Searching messages...</p>
        </div>
      </div>
    );
  }

  if (!searchResult) {
    return (
      <div className={`p-4 ${className}`}>
        <div className="text-center py-8 text-gray-500">
          <svg className="w-12 h-12 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <p>Start typing to search messages...</p>
        </div>
      </div>
    );
  }

  if (searchResult.messages.length === 0) {
    return (
      <div className={`p-4 ${className}`}>
        <div className="text-center py-8">
          <svg className="w-12 h-12 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No results found</h3>
          <p className="text-gray-500">
            No messages found for "{searchResult.query}"
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={`flex flex-col h-full ${className}`}>
      {/* Results header */}
      <div className="p-4 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-indigo-50">
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center space-x-2">
              <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <h3 className="font-semibold text-gray-900">Search Results</h3>
            </div>
            <p className="text-sm text-gray-600 mt-1">
              <span className="font-medium text-blue-700">{searchResult.totalCount}</span> {searchResult.totalCount === 1 ? 'result' : 'results'} for 
              <span className="font-medium text-gray-800"> "{searchResult.query}"</span>
            </p>
          </div>
          {searchResult.currentPage > 0 && (
            <div className="text-xs text-gray-500 bg-white px-2 py-1 rounded-full border">
              Page {searchResult.currentPage + 1}
            </div>
          )}
        </div>
      </div>

      {/* Results list */}
      <div className="flex-1 overflow-y-auto">
        <div className="divide-y divide-gray-100">
          {searchResult.messages.map((result, index) => (
            <div
              key={`${result.id}-${index}`}
              className={`group relative transition-all duration-200 ${
                selectedResult === result.id
                  ? 'bg-blue-50 border-l-4 border-blue-500'
                  : 'hover:bg-gray-50 hover:shadow-sm'
              }`}
            >
              <div className="p-4 cursor-pointer" onClick={() => {
                setSelectedResult(result.id);
                onJumpToMessage(result.id);
              }}>
                <div className="flex items-start space-x-3">
                  {/* User Avatar */}
                  <div className="flex-shrink-0">
                    <div className="w-9 h-9 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center shadow-md">
                      <span className="text-white text-sm font-semibold">
                        {result.senderUsername.charAt(0).toUpperCase()}
                      </span>
                    </div>
                  </div>
                  
                  {/* Message Content */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-2 mb-2">
                      <span className="font-semibold text-gray-900 text-sm">
                        {result.senderUsername}
                      </span>
                      <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                        {formatRelativeTime(result.timestamp)}
                      </span>
                      {result.score && result.score > 0.7 && (
                        <span className="text-xs text-green-600 bg-green-100 px-2 py-0.5 rounded-full font-medium">
                          High match
                        </span>
                      )}
                    </div>
                    
                    {/* Highlighted message content */}
                    <div className="message-content mb-2">
                      {renderHighlightedContent(result)}
                    </div>
                    
                    {/* Timestamp and actions */}
                    <div className="flex items-center justify-between text-xs text-gray-400">
                      <span>{formatTimestamp(result.timestamp)}</span>
                      <div className="flex items-center space-x-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        {onShowContext && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              onShowContext(result.id);
                            }}
                            className="flex items-center space-x-1 text-blue-600 hover:text-blue-700 font-medium"
                          >
                            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <span>Context</span>
                          </button>
                        )}
                        <button className="flex items-center space-x-1 text-gray-500 hover:text-gray-700">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                          </svg>
                          <span>Jump</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Load more button */}
        {searchResult.hasMore && onLoadMore && (
          <div className="p-4 border-t border-gray-100">
            <button
              onClick={onLoadMore}
              disabled={isLoading}
              className="w-full py-3 px-4 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-lg text-sm font-medium hover:from-blue-600 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-md hover:shadow-lg"
            >
              {isLoading ? (
                <div className="flex items-center justify-center space-x-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  <span>Loading more results...</span>
                </div>
              ) : (
                <div className="flex items-center justify-center space-x-2">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 13l-7 7-7-7m14-8l-7 7-7-7" />
                  </svg>
                  <span>Load {searchResult.totalCount - searchResult.messages.length} more results</span>
                </div>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchResultsList;
import React from 'react';
import { MessageSearchResult, SearchResult } from '../services/api';

interface SearchResultsListProps {
  searchResult: SearchResult | null;
  isLoading: boolean;
  error: string | null;
  onJumpToMessage: (messageId: string) => void;
  onLoadMore?: () => void;
  className?: string;
}

const SearchResultsList: React.FC<SearchResultsListProps> = ({
  searchResult,
  isLoading,
  error,
  onJumpToMessage,
  onLoadMore,
  className = ''
}) => {
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
          className="text-sm text-gray-700"
          dangerouslySetInnerHTML={{ 
            __html: result.highlightedContent.replace(
              /<mark>/g, 
              '<mark class="bg-yellow-200 px-1 rounded">'
            )
          }}
        />
      );
    }
    
    return <div className="text-sm text-gray-700">{result.content}</div>;
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
      <div className="p-4 border-b border-gray-200 bg-gray-50">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-medium text-gray-900">Search Results</h3>
            <p className="text-sm text-gray-500">
              {searchResult.totalCount} {searchResult.totalCount === 1 ? 'result' : 'results'} for "{searchResult.query}"
            </p>
          </div>
          {searchResult.currentPage > 0 && (
            <div className="text-xs text-gray-500">
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
              className="p-4 hover:bg-gray-50 cursor-pointer transition-colors"
              onClick={() => onJumpToMessage(result.id)}
            >
              <div className="flex items-start space-x-3">
                {/* User Avatar */}
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm font-medium">
                      {result.senderUsername.charAt(0).toUpperCase()}
                    </span>
                  </div>
                </div>
                
                {/* Message Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-2 mb-1">
                    <span className="font-medium text-gray-900 text-sm">
                      {result.senderUsername}
                    </span>
                    <span className="text-xs text-gray-500">
                      {formatTimestamp(result.timestamp)}
                    </span>
                    {result.score && (
                      <span className="text-xs text-gray-400">
                        Score: {Math.round(result.score * 100)}%
                      </span>
                    )}
                  </div>
                  
                  {/* Highlighted message content */}
                  <div className="message-content">
                    {renderHighlightedContent(result)}
                  </div>
                </div>
                
                {/* Jump indicator */}
                <div className="flex-shrink-0">
                  <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
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
              className="w-full py-2 px-4 border border-gray-300 rounded-lg text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isLoading ? (
                <div className="flex items-center justify-center space-x-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-400"></div>
                  <span>Loading...</span>
                </div>
              ) : (
                `Load more results (${searchResult.totalCount - searchResult.messages.length} remaining)`
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchResultsList;
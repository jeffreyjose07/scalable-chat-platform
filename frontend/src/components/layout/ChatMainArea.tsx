import React from 'react';
import { User, ChatMessage } from '../../types/chat';
import MessageList from '../MessageList';
import MessageInput from '../MessageInput';
import NewMessageSearchBar from '../NewMessageSearchBar';
import SearchResultsList from '../SearchResultsList';

interface ChatMainAreaProps {
    isSearchMode: boolean;
    toggleSearchMode: () => void;
    setIsMobileSidebarOpen: (isOpen: boolean) => void;
    selectedConversation: string;
    conversationMessages: ChatMessage[];
    isLoadingMessages: boolean;
    isConnected: boolean;
    handleSendMessage: (content: string) => void;
    searchResult: any;
    isSearchLoading: boolean;
    searchError: string | null;
    jumpToMessage: (messageId: string) => void;
    loadMoreResults: (conversationId: string) => void;
    performSearch: (conversationId: string, query: string, filters: any) => void;
    clearSearch: () => void;
    isCurrentConversationGroup: boolean;
    setIsGroupSettingsModalOpen: (isOpen: boolean) => void;
    getConversationDisplayName: (id: string) => string;
    user: User | null;
    isLoadingConversations: boolean;
}

const ChatMainArea: React.FC<ChatMainAreaProps> = ({
    isSearchMode,
    toggleSearchMode,
    setIsMobileSidebarOpen,
    selectedConversation,
    conversationMessages,
    isLoadingMessages,
    isConnected,
    handleSendMessage,
    searchResult,
    isSearchLoading,
    searchError,
    jumpToMessage,
    loadMoreResults,
    performSearch,
    clearSearch,
    isCurrentConversationGroup,
    setIsGroupSettingsModalOpen,
    getConversationDisplayName,
    user,
    isLoadingConversations
}) => {
    return (
        <div className="flex-1 flex lg:ml-0 min-h-0">
            {/* Chat Column */}
            <div className={`flex flex-col transition-all duration-300 min-h-0 w-full ${isSearchMode ? 'lg:w-1/2' : 'w-full'
                }`}>
                {/* Chat Header - Fixed at top */}
                <div className="flex-shrink-0 px-4 sm:px-6 py-3 border-b border-gray-200 dark:border-gray-700 bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm z-10">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center min-w-0 flex-1">
                            <button
                                onClick={() => setIsMobileSidebarOpen(true)}
                                className="lg:hidden p-2 -ml-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg mr-2 flex-shrink-0 transition-colors"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                                </svg>
                            </button>

                            <div className="min-w-0 flex-1">
                                <div className="flex items-center">
                                    {selectedConversation && (() => {
                                        const displayName = getConversationDisplayName(selectedConversation);
                                        const hue = displayName.charCodeAt(0) * 7 % 360;
                                        const saturation = 75;
                                        const lightness = 45;
                                        const avatarColor = `hsl(${hue}, ${saturation}%, ${lightness}%)`;

                                        return (
                                            <div
                                                className="w-10 h-10 rounded-full flex items-center justify-center text-white font-semibold mr-3 shadow-md border-2 border-white/20 dark:border-gray-600/30"
                                                style={{ background: `linear-gradient(135deg, ${avatarColor}, hsl(${hue}, ${saturation}%, ${lightness - 10}%))` }}
                                            >
                                                {displayName.charAt(0).toUpperCase()}
                                            </div>
                                        );
                                    })()}
                                    <div>
                                        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 truncate">
                                            {selectedConversation ? getConversationDisplayName(selectedConversation) : 'No conversation selected'}
                                        </h2>
                                        <div className="text-sm text-gray-500 dark:text-gray-400 flex items-center">
                                            <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                            </svg>
                                            {conversationMessages.length} messages
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="flex items-center space-x-2 flex-shrink-0 relative">
                            {/* Group Settings Button */}
                            {isCurrentConversationGroup && (
                                <button
                                    onClick={() => setIsGroupSettingsModalOpen(true)}
                                    className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-md"
                                    title="Group Settings"
                                >
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                    </svg>
                                </button>
                            )}

                            <NewMessageSearchBar
                                isSearchMode={isSearchMode}
                                onToggleSearch={toggleSearchMode}
                                onSearch={(query, filters) => {
                                    if (!selectedConversation) {
                                        console.error('❌ No conversation selected for search');
                                        return;
                                    }
                                    performSearch(selectedConversation, query, filters);
                                }}
                                onClearSearch={clearSearch}
                                isLoading={isSearchLoading}
                                resultsCount={searchResult?.totalCount}
                                enableFilters={true}
                            />
                        </div>
                    </div>
                </div>

                {/* Messages - Scrollable Area */}
                <div className="flex-1 overflow-y-auto px-4 sm:px-6 py-4" style={{
                    WebkitOverflowScrolling: 'touch',
                    scrollBehavior: 'smooth'
                }}>
                    <MessageList
                        messages={conversationMessages}
                        currentUserId={user?.id}
                        isLoading={isLoadingMessages || isLoadingConversations || !isConnected}
                    />
                </div>

                {/* Message Input - Fixed at Bottom */}
                <div className="flex-shrink-0 border-t border-gray-200 dark:border-gray-700 bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm">
                    <MessageInput
                        key={selectedConversation}
                        onSendMessage={handleSendMessage}
                        disabled={!isConnected || !selectedConversation}
                    />
                </div>
            </div>

            {/* Search Results Panel - Desktop: Side panel, Mobile: Overlay */}
            {isSearchMode && (
                <>
                    {/* Desktop: Side panel */}
                    <div className="hidden lg:flex lg:w-1/2 border-l border-gray-200">
                        <SearchResultsList
                            searchResult={searchResult}
                            isLoading={isSearchLoading}
                            error={searchError}
                            onJumpToMessage={jumpToMessage}
                            onLoadMore={searchResult?.hasMore ? () => loadMoreResults(selectedConversation) : undefined}
                            className="w-full"
                        />
                    </div>

                    {/* Mobile: Full screen overlay */}
                    <div className="lg:hidden fixed inset-0 bg-white z-50 flex flex-col">
                        {/* Mobile search header */}
                        <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-white">
                            <h2 className="text-lg font-semibold text-gray-900">Search Results</h2>
                            <button
                                onClick={toggleSearchMode}
                                className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        {/* Search results content */}
                        <div className="flex-1 overflow-hidden">
                            <SearchResultsList
                                searchResult={searchResult}
                                isLoading={isSearchLoading}
                                error={searchError}
                                onJumpToMessage={(messageId) => {
                                    jumpToMessage(messageId);
                                    // Close search on mobile after jumping
                                    toggleSearchMode();
                                }}
                                onLoadMore={searchResult?.hasMore ? () => loadMoreResults(selectedConversation) : undefined}
                                className="h-full"
                            />
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default ChatMainArea;

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
    const getAvatarHue = (name: string) => name.charCodeAt(0) * 7 % 360;

    return (
        <div className="flex-1 flex lg:ml-0 min-h-0">
            {/* Chat column */}
            <div className={`flex flex-col transition-all duration-300 min-h-0 w-full ${isSearchMode ? 'lg:w-1/2' : 'w-full'}`}>

                {/* Header */}
                <div className="flex-shrink-0 px-4 sm:px-5 py-3 border-b border-gray-100 dark:border-zinc-800 bg-white/95 dark:bg-gray-900/95 backdrop-blur-sm z-10">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center min-w-0 flex-1">
                            {/* Mobile hamburger */}
                            <button
                                onClick={() => setIsMobileSidebarOpen(true)}
                                className="lg:hidden p-2 -ml-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-lg mr-2 flex-shrink-0 transition-colors"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                                </svg>
                            </button>

                            {/* Conversation avatar + name */}
                            <div className="min-w-0 flex-1">
                                <div className="flex items-center space-x-3">
                                    {selectedConversation && (() => {
                                        const displayName = getConversationDisplayName(selectedConversation);
                                        const hue = getAvatarHue(displayName);
                                        return (
                                            <div
                                                className="w-9 h-9 rounded-full flex items-center justify-center text-white font-semibold text-sm flex-shrink-0 shadow-sm"
                                                style={{ background: `hsl(${hue}, 65%, 48%)` }}
                                            >
                                                {displayName.charAt(0).toUpperCase()}
                                            </div>
                                        );
                                    })()}
                                    <div className="min-w-0">
                                        <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
                                            {selectedConversation
                                                ? getConversationDisplayName(selectedConversation)
                                                : 'No conversation selected'
                                            }
                                        </h2>
                                        <div className="text-xs text-gray-400 dark:text-zinc-500">
                                            {conversationMessages.length} {conversationMessages.length === 1 ? 'message' : 'messages'}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Actions */}
                        <div className="flex items-center space-x-1 flex-shrink-0">
                            {isCurrentConversationGroup && (
                                <button
                                    onClick={() => setIsGroupSettingsModalOpen(true)}
                                    className="p-2 text-gray-400 hover:text-green-600 dark:hover:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/20 rounded-lg transition-colors"
                                    title="Group Settings"
                                >
                                    <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" style={{ width: '18px', height: '18px' }}>
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

                {/* Messages */}
                <div
                    className="flex-1 overflow-y-auto px-4 sm:px-5 py-4"
                    style={{ WebkitOverflowScrolling: 'touch', scrollBehavior: 'smooth' }}
                >
                    <MessageList
                        messages={conversationMessages}
                        currentUserId={user?.id}
                        isLoading={isLoadingMessages || isLoadingConversations || !isConnected}
                    />
                </div>

                {/* Input */}
                <div className="flex-shrink-0 border-t border-gray-100 dark:border-zinc-800 bg-white/95 dark:bg-gray-800/95 backdrop-blur-sm">
                    <MessageInput
                        key={selectedConversation}
                        onSendMessage={handleSendMessage}
                        disabled={!isConnected || !selectedConversation}
                    />
                </div>
            </div>

            {/* Search Results Panel */}
            {isSearchMode && (
                <>
                    <div className="hidden lg:flex lg:w-1/2 border-l border-gray-100 dark:border-zinc-800">
                        <SearchResultsList
                            searchResult={searchResult}
                            isLoading={isSearchLoading}
                            error={searchError}
                            onJumpToMessage={jumpToMessage}
                            onLoadMore={searchResult?.hasMore ? () => loadMoreResults(selectedConversation) : undefined}
                            className="w-full"
                        />
                    </div>

                    <div className="lg:hidden fixed inset-0 bg-white dark:bg-zinc-950 z-50 flex flex-col">
                        <div className="flex items-center justify-between p-4 border-b border-gray-100 dark:border-zinc-800">
                            <h2 className="text-base font-semibold text-gray-900 dark:text-gray-100">Search Results</h2>
                            <button
                                onClick={toggleSearchMode}
                                className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-lg transition-colors"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>
                        <div className="flex-1 overflow-hidden">
                            <SearchResultsList
                                searchResult={searchResult}
                                isLoading={isSearchLoading}
                                error={searchError}
                                onJumpToMessage={(messageId) => { jumpToMessage(messageId); toggleSearchMode(); }}
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

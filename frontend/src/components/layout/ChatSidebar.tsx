import React, { useState, useRef, useEffect } from 'react';
import { User } from '../../types/chat';
import { ConversationType } from '../ConversationTypeToggle';
import ConversationList from '../ConversationList';
import ThemeToggle from '../ThemeToggle';
import VersionInfo from '../VersionInfo';

interface ChatSidebarProps {
    isMobileSidebarOpen: boolean;
    setIsMobileSidebarOpen: (isOpen: boolean) => void;
    user: User | null;
    logout: () => void;
    isConnected: boolean;
    isReconnecting: boolean;
    selectedConversation: string;
    handleConversationSelect: (id: string) => void;
    conversations: any[];
    activeConversationType: ConversationType;
    handleConversationTypeChange: (type: ConversationType) => void;
    onNewDirectMessage: () => void;
    onNewGroup: () => void;
    unreadCounts: Record<string, number>;
    handleDeleteConversation: (id: string) => void;
    setIsUserSettingsModalOpen: (isOpen: boolean) => void;
}

const ChatSidebar: React.FC<ChatSidebarProps> = ({
    isMobileSidebarOpen,
    setIsMobileSidebarOpen,
    user,
    logout,
    isConnected,
    isReconnecting,
    selectedConversation,
    handleConversationSelect,
    conversations,
    activeConversationType,
    handleConversationTypeChange,
    onNewDirectMessage,
    onNewGroup,
    unreadCounts,
    handleDeleteConversation,
    setIsUserSettingsModalOpen
}) => {
    const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
    const userMenuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
                setIsUserMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => { document.removeEventListener('mousedown', handleClickOutside); };
    }, []);

    const getUserAvatarColor = (displayName: string) => {
        const hue = displayName.charCodeAt(0) * 7 % 360;
        return `hsl(${hue}, 65%, 50%)`;
    };

    return (
        <>
            {isMobileSidebarOpen && (
                <div
                    className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 lg:hidden"
                    onClick={() => setIsMobileSidebarOpen(false)}
                />
            )}

            <div className={`
                fixed lg:relative lg:translate-x-0 z-50 lg:z-0
                w-80 lg:w-72 xl:w-80
                bg-white dark:bg-zinc-950
                border-r border-gray-100 dark:border-zinc-800
                ${isMobileSidebarOpen ? 'h-screen' : 'h-full'}
                flex flex-col
                transition-transform duration-300 ease-in-out
                ${isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'}
            `}>
                {/* Header */}
                <div className="px-4 pt-4 pb-3 border-b border-gray-100 dark:border-zinc-800/80 flex-shrink-0">
                    <div className="flex items-center justify-between">
                        {/* Brand */}
                        <div className="flex items-center space-x-2 min-w-0">
                            <div className="w-8 h-8 bg-green-600 rounded-lg flex items-center justify-center flex-shrink-0 shadow-sm">
                                <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                                    <path d="M2 5a2 2 0 012-2h7a2 2 0 012 2v4a2 2 0 01-2 2H9l-3 3v-3H4a2 2 0 01-2-2V5z" />
                                    <path d="M15 7v2a4 4 0 01-4 4H9.828l-1.766 1.767c.28.149.599.233.938.233h2l3 3v-3h2a2 2 0 002-2V9a2 2 0 00-2-2h-1z" />
                                </svg>
                            </div>
                            <div className="min-w-0">
                                <h1 className="font-display text-lg font-semibold text-gray-900 dark:text-gray-50 leading-none tracking-tight">
                                    Chat Platform
                                </h1>
                                <VersionInfo className="mt-0.5" clickable />
                            </div>
                        </div>

                        {/* Controls */}
                        <div className="flex items-center space-x-1 flex-shrink-0">
                            <ThemeToggle />

                            {/* User menu */}
                            <div className="relative" ref={userMenuRef}>
                                <button
                                    onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                                    className="flex items-center space-x-1.5 px-2 py-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-zinc-800 transition-colors"
                                    title="User Menu"
                                >
                                    {user && (() => {
                                        const displayName = user.displayName || user.username;
                                        return (
                                            <div
                                                className="w-6 h-6 rounded-full flex items-center justify-center text-white text-xs font-semibold"
                                                style={{ backgroundColor: getUserAvatarColor(displayName) }}
                                            >
                                                {displayName.charAt(0).toUpperCase()}
                                            </div>
                                        );
                                    })()}
                                    <svg className={`w-3.5 h-3.5 text-gray-400 transition-transform ${isUserMenuOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                    </svg>
                                </button>

                                {isUserMenuOpen && (
                                    <div className="absolute right-0 mt-2 w-56 bg-white dark:bg-zinc-900 rounded-xl shadow-xl border border-gray-100 dark:border-zinc-800 py-2 z-50 animate-slideUp">
                                        <div className="px-4 py-3 border-b border-gray-100 dark:border-zinc-800">
                                            <div className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                                                {user?.displayName || user?.username}
                                            </div>
                                            <div className="text-xs text-gray-400 dark:text-zinc-500 mt-0.5">
                                                {user?.email}
                                            </div>
                                        </div>

                                        <button
                                            onClick={() => { setIsUserSettingsModalOpen(true); setIsUserMenuOpen(false); }}
                                            className="w-full text-left px-4 py-2.5 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-zinc-800 flex items-center space-x-2.5 transition-colors"
                                        >
                                            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            </svg>
                                            <span>Settings</span>
                                        </button>

                                        <div className="my-1 border-t border-gray-100 dark:border-zinc-800" />

                                        <button
                                            onClick={() => { logout(); setIsUserMenuOpen(false); }}
                                            className="w-full text-left px-4 py-2.5 text-sm text-red-500 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center space-x-2.5 transition-colors"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                                            </svg>
                                            <span>Sign Out</span>
                                        </button>
                                    </div>
                                )}
                            </div>

                            <button
                                onClick={() => { setIsMobileSidebarOpen(false); setIsUserMenuOpen(false); }}
                                className="lg:hidden p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-zinc-800 rounded-lg transition-colors"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>
                    </div>

                    {/* Connection status */}
                    <div className="flex items-center mt-3">
                        <div className={`w-2 h-2 rounded-full mr-2 ${
                            isConnected ? 'bg-emerald-500 animate-pulse' :
                            isReconnecting ? 'bg-green-500 animate-pulse' : 'bg-red-500'
                        }`} />
                        <span className={`text-xs font-medium ${
                            isConnected ? 'text-emerald-600 dark:text-emerald-400' :
                            isReconnecting ? 'text-green-500 dark:text-green-400' : 'text-red-500 dark:text-red-400'
                        }`}>
                            {isConnected ? 'Connected' : isReconnecting ? 'Reconnecting…' : 'Disconnected'}
                        </span>
                    </div>
                </div>

                <ConversationList
                    selectedConversation={selectedConversation}
                    onSelectConversation={handleConversationSelect}
                    conversations={conversations}
                    activeType={activeConversationType}
                    onTypeChange={handleConversationTypeChange}
                    onNewDirectMessage={onNewDirectMessage}
                    onNewGroup={onNewGroup}
                    unreadCounts={unreadCounts}
                    currentUserId={user?.id}
                    onDeleteConversation={handleDeleteConversation}
                />
            </div>
        </>
    );
};

export default ChatSidebar;

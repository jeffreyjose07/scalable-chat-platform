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

    // Close user menu when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
                setIsUserMenuOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    return (
        <>
            {/* Mobile Sidebar Overlay */}
            {isMobileSidebarOpen && (
                <div
                    className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
                    onClick={() => setIsMobileSidebarOpen(false)}
                />
            )}

            {/* Sidebar - Conversations */}
            <div className={`
        fixed lg:relative lg:translate-x-0 z-50 lg:z-0
        w-80 lg:w-72 xl:w-80 bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700 
        ${isMobileSidebarOpen ? 'h-screen' : 'h-full'}
        flex flex-col shadow-lg lg:shadow-none
        transition-transform duration-300 ease-in-out
        ${isMobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
                <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex-shrink-0 bg-white dark:bg-gray-800">
                    <div className="flex items-center justify-between">
                        <h1 className="text-lg lg:text-xl font-bold text-gray-900 dark:text-gray-100 flex items-center">
                            <div className="w-8 h-8 bg-gradient-to-br from-green-500 to-green-600 rounded-lg flex items-center justify-center mr-2 shadow-md">
                                <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                                    <path d="M2 5a2 2 0 012-2h7a2 2 0 012 2v4a2 2 0 01-2 2H9l-3 3v-3H4a2 2 0 01-2-2V5z" />
                                    <path d="M15 7v2a4 4 0 01-4 4H9.828l-1.766 1.767c.28.149.599.233.938.233h2l3 3v-3h2a2 2 0 002-2V9a2 2 0 00-2-2h-1z" />
                                </svg>
                            </div>
                            Chat Platform
                            <VersionInfo className="ml-2" clickable />
                        </h1>
                        <div className="flex items-center space-x-2">
                            <ThemeToggle />

                            {/* User Menu Dropdown */}
                            <div className="relative" ref={userMenuRef}>
                                <button
                                    onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                                    className="flex items-center space-x-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-white/70 dark:hover:bg-gray-600/70 px-3 py-2 rounded-lg transition-colors"
                                    title="User Menu"
                                >
                                    {user && (() => {
                                        const displayName = user.displayName || user.username;
                                        const hue = displayName.charCodeAt(0) * 7 % 360;
                                        const saturation = 75;
                                        const lightness = 50;
                                        const avatarColor = `hsl(${hue}, ${saturation}%, ${lightness}%)`;

                                        return (
                                            <div
                                                className="w-6 h-6 rounded-full flex items-center justify-center text-white text-xs font-semibold shadow-sm"
                                                style={{ backgroundColor: avatarColor }}
                                            >
                                                {displayName.charAt(0).toUpperCase()}
                                            </div>
                                        );
                                    })()}
                                    <svg className={`w-4 h-4 transition-transform ${isUserMenuOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                    </svg>
                                </button>

                                {/* Dropdown Menu */}
                                {isUserMenuOpen && (
                                    <div className="absolute right-0 mt-2 w-56 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-2 z-50">
                                        {/* User Info */}
                                        <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700">
                                            <div className="text-sm font-medium text-gray-900 dark:text-gray-100">
                                                {user?.displayName || user?.username}
                                            </div>
                                            <div className="text-xs text-gray-500 dark:text-gray-400">
                                                {user?.email}
                                            </div>
                                        </div>

                                        {/* Menu Items */}
                                        <button
                                            onClick={() => {
                                                setIsUserSettingsModalOpen(true);
                                                setIsUserMenuOpen(false);
                                            }}
                                            className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center space-x-2"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            </svg>
                                            <span>Settings</span>
                                        </button>

                                        <div className="border-t border-gray-200 dark:border-gray-700 my-1"></div>

                                        <button
                                            onClick={() => {
                                                logout();
                                                setIsUserMenuOpen(false);
                                            }}
                                            className="w-full text-left px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center space-x-2"
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
                                onClick={() => {
                                    setIsMobileSidebarOpen(false);
                                    setIsUserMenuOpen(false);
                                }}
                                className="lg:hidden p-1 text-gray-500 hover:text-gray-700 hover:bg-white/50 rounded-md transition-colors"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>
                    </div>
                    <div className="flex items-center mt-3">
                        <div className={`w-3 h-3 rounded-full mr-2 shadow-sm ${isConnected ? 'bg-green-500 animate-pulse' :
                                isReconnecting ? 'bg-yellow-500 animate-spin' : 'bg-red-500'
                            }`}></div>
                        <span className={`text-sm font-medium ${isConnected ? 'text-green-700 dark:text-green-400' :
                                isReconnecting ? 'text-yellow-600 dark:text-yellow-400' : 'text-red-600 dark:text-red-400'
                            }`}>
                            {isConnected ? 'Connected' : isReconnecting ? 'Reconnecting...' : 'Disconnected'}
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

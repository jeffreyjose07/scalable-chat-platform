import React, { useState, useEffect, useRef } from 'react';
import { User } from '../types/chat';

interface UserSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectUser: (user: User) => void;
  searchUsers: (query: string) => Promise<User[]>;
  getUserSuggestions: () => Promise<User[]>;
  currentUserId?: string;
}

const UserSearchModal: React.FC<UserSearchModalProps> = ({
  isOpen,
  onClose,
  onSelectUser,
  searchUsers,
  getUserSuggestions,
  currentUserId
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const searchTimeoutRef = useRef<NodeJS.Timeout>();

  // Focus input when modal opens
  useEffect(() => {
    if (isOpen && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isOpen]);

  // Load suggestions when modal opens
  useEffect(() => {
    if (isOpen && searchQuery === '') {
      loadSuggestions();
    }
  }, [isOpen]);

  // Debounced search
  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    if (searchQuery.trim()) {
      searchTimeoutRef.current = setTimeout(() => {
        performSearch(searchQuery.trim());
      }, 300);
    } else if (isOpen) {
      loadSuggestions();
    }

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [searchQuery]);

  const loadSuggestions = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const suggestions = await getUserSuggestions();
      // Filter out the current user to prevent self-messaging
      const filteredSuggestions = suggestions.filter(user => user.id !== currentUserId);
      setUsers(filteredSuggestions);
    } catch (err) {
      setError('Failed to load user suggestions');
      setUsers([]);
    } finally {
      setIsLoading(false);
    }
  };

  const performSearch = async (query: string) => {
    setIsLoading(true);
    setError(null);
    try {
      const results = await searchUsers(query);
      // Filter out the current user to prevent self-messaging
      const filteredResults = results.filter(user => user.id !== currentUserId);
      setUsers(filteredResults);
    } catch (err) {
      setError('Failed to search users');
      setUsers([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUserSelect = (user: User) => {
    onSelectUser(user);
    handleClose();
  };

  const handleClose = () => {
    setSearchQuery('');
    setUsers([]);
    setError(null);
    onClose();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      handleClose();
    }
  };

  const getUserDisplayName = (user: User) => {
    return user.displayName || user.username;
  };

  const getUserAvatar = (user: User) => {
    if (user.avatarUrl) {
      return (
        <img 
          src={user.avatarUrl} 
          alt={getUserDisplayName(user)}
          className="w-10 h-10 rounded-full object-cover"
        />
      );
    }
    
    // Generate consistent avatar colors
    const displayName = getUserDisplayName(user);
    const safeDisplayName = String(displayName || 'U');
    const hue = safeDisplayName.charCodeAt(0) * 7 % 360;
    const saturation = 70;
    const lightness = 55;
    
    const backgroundColor = `hsl(${hue}, ${saturation}%, ${lightness}%)`;
    const textColor = lightness > 60 ? '#374151' : '#ffffff'; // gray-700 or white
    
    return (
      <div 
        className="w-10 h-10 rounded-full flex items-center justify-center"
        style={{ backgroundColor }}
      >
        <span className="font-medium" style={{ color: textColor }}>
          {safeDisplayName.charAt(0).toUpperCase()}
        </span>
      </div>
    );
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md max-h-[80vh] flex flex-col">
        {/* Header */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">New Direct Message</h2>
            <button
              onClick={handleClose}
              className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          
          {/* Search Input */}
          <div className="mt-4 relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              ref={searchInputRef}
              type="text"
              placeholder="Search for users..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto">
          {error && (
            <div className="p-4 text-center text-red-600">
              <svg className="w-8 h-8 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {error}
            </div>
          )}

          {isLoading && (
            <div className="p-8 text-center">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
              <div className="mt-2 text-gray-600">
                {searchQuery ? 'Searching...' : 'Loading suggestions...'}
              </div>
            </div>
          )}

          {!isLoading && !error && users.length === 0 && (
            <div className="p-8 text-center text-gray-500">
              {searchQuery ? 'No users found' : 'No user suggestions available'}
            </div>
          )}

          {!isLoading && !error && users.length > 0 && (
            <div className="p-2">
              {!searchQuery && (
                <div className="px-2 py-1 text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Suggested Users
                </div>
              )}
              <div className="space-y-1">
                {users.map((user) => (
                  <button
                    key={user.id}
                    onClick={() => handleUserSelect(user)}
                    className="w-full flex items-center p-3 rounded-lg hover:bg-gray-50 transition-colors text-left"
                  >
                    <div className="flex-shrink-0 mr-3">
                      {getUserAvatar(user)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-gray-900 truncate">
                        {getUserDisplayName(user)}
                      </div>
                      {user.username !== getUserDisplayName(user) && (
                        <div className="text-sm text-gray-500 truncate">
                          @{user.username}
                        </div>
                      )}
                      {user.email && (
                        <div className="text-xs text-gray-400 truncate">
                          {user.email}
                        </div>
                      )}
                    </div>
                    <div className="flex-shrink-0 ml-2">
                      <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                      </svg>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserSearchModal;
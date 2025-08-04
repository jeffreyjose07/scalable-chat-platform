import React, { useState } from 'react';
import { User, CreateGroupRequest } from '../../types/chat';
import { useUsers } from '../../hooks/useUsers';
import { api } from '../../services/api';

interface CreateGroupModalProps {
  isOpen: boolean;
  onClose: () => void;
  onGroupCreated?: (group: any) => void;
}

export const CreateGroupModal: React.FC<CreateGroupModalProps> = ({
  isOpen,
  onClose,
  onGroupCreated,
}) => {
  const [groupName, setGroupName] = useState('');
  const [description, setDescription] = useState('');
  const [isPublic, setIsPublic] = useState(false);
  const [maxParticipants, setMaxParticipants] = useState(100);
  const [selectedUsers, setSelectedUsers] = useState<string[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { users } = useUsers();

  const filteredUsers = users.filter(user => 
    user.displayName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    user.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleUserToggle = (userId: string) => {
    setSelectedUsers(prev => 
      prev.includes(userId) 
        ? prev.filter(id => id !== userId)
        : [...prev, userId]
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    if (!groupName.trim()) {
      setError('Group name is required');
      return;
    }

    if (maxParticipants < 2) {
      setError('Group must have at least 2 participants');
      return;
    }

    if (maxParticipants > 1000) {
      setError('Group cannot have more than 1000 participants');
      return;
    }

    setIsLoading(true);

    try {
      const createGroupRequest: CreateGroupRequest = {
        name: groupName.trim(),
        description: description.trim() || null,
        isPublic,
        maxParticipants,
        participantIds: selectedUsers,
      };

      const response = await api.conversation.createGroup(createGroupRequest);
      
      if (onGroupCreated) {
        onGroupCreated(response);
      }

      // Reset form
      setGroupName('');
      setDescription('');
      setIsPublic(false);
      setMaxParticipants(100);
      setSelectedUsers([]);
      setSearchTerm('');
      
      onClose();
    } catch (error: any) {
      console.error('Failed to create group:', error);
      setError(error.message || 'Failed to create group');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setGroupName('');
    setDescription('');
    setIsPublic(false);
    setMaxParticipants(100);
    setSelectedUsers([]);
    setSearchTerm('');
    setError(null);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-full max-w-md mx-4 max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <h2 className="text-xl font-bold mb-4">Create New Group</h2>
          
          {error && (
            <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Group Name */}
            <div>
              <label htmlFor="groupName" className="block text-sm font-medium text-gray-700 mb-1">
                Group Name *
              </label>
              <input
                type="text"
                id="groupName"
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter group name"
                maxLength={100}
                required
              />
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                Description (optional)
              </label>
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter group description"
                rows={3}
                maxLength={500}
              />
            </div>

            {/* Privacy Setting */}
            <div className="flex items-center">
              <input
                type="checkbox"
                id="isPublic"
                checked={isPublic}
                onChange={(e) => setIsPublic(e.target.checked)}
                className="mr-2"
              />
              <label htmlFor="isPublic" className="text-sm font-medium text-gray-700">
                Make group public
              </label>
            </div>

            {/* Max Participants */}
            <div>
              <label htmlFor="maxParticipants" className="block text-sm font-medium text-gray-700 mb-1">
                Maximum Participants
              </label>
              <input
                type="number"
                id="maxParticipants"
                value={maxParticipants}
                onChange={(e) => setMaxParticipants(parseInt(e.target.value) || 100)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                min={2}
                max={1000}
              />
            </div>

            {/* User Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Add Participants (optional)
              </label>
              <div className="mb-2">
                <input
                  type="text"
                  placeholder="Search participants..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                />
              </div>
              <div className="max-h-40 overflow-y-auto border border-gray-300 rounded-md p-2">
                {users.length === 0 ? (
                  <p className="text-gray-500 text-sm">Loading users...</p>
                ) : filteredUsers.length === 0 ? (
                  <p className="text-gray-500 text-sm">No participants found</p>
                ) : (
                  filteredUsers.map((user: User) => (
                    <div key={user.id} className="flex items-center p-2 hover:bg-gray-100 rounded">
                      <input
                        type="checkbox"
                        checked={selectedUsers.includes(user.id)}
                        onChange={() => handleUserToggle(user.id)}
                        className="mr-2"
                      />
                      <div className="flex-1">
                        <div className="text-sm font-medium">{user.displayName}</div>
                        <div className="text-xs text-gray-500">{user.email}</div>
                      </div>
                      {user.isOnline && (
                        <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                      )}
                    </div>
                  ))
                )}
              </div>
              {selectedUsers.length > 0 && (
                <p className="text-sm text-gray-600 mt-1">
                  {selectedUsers.length} participant{selectedUsers.length !== 1 ? 's' : ''} selected
                </p>
              )}
            </div>

            {/* Action Buttons */}
            <div className="flex justify-end space-x-3 pt-4">
              <button
                type="button"
                onClick={handleCancel}
                className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500"
                disabled={isLoading}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isLoading || !groupName.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                {isLoading ? 'Creating...' : 'Create Group'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};
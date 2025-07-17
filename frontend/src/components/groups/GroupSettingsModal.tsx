import React, { useState, useEffect } from 'react';
import { User, ConversationDto, UpdateGroupSettingsRequest, ParticipantRole } from '../../types/chat';
import { useUsers } from '../../hooks/useUsers';
import { api } from '../../services/api';

interface GroupSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  conversation: ConversationDto;
  userRole: ParticipantRole;
  onGroupUpdated?: (group: ConversationDto) => void;
  onGroupDeleted?: () => void;
}

export const GroupSettingsModal: React.FC<GroupSettingsModalProps> = ({
  isOpen,
  onClose,
  conversation,
  userRole,
  onGroupUpdated,
  onGroupDeleted,
}) => {
  const [groupName, setGroupName] = useState(conversation.name || '');
  const [description, setDescription] = useState(conversation.description || '');
  const [isPublic, setIsPublic] = useState(conversation.isPublic || false);
  const [maxParticipants, setMaxParticipants] = useState(conversation.maxParticipants || 100);
  const [selectedUsers, setSelectedUsers] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'settings' | 'participants'>('settings');

  const { users } = useUsers();

  // Check if user can manage participants or update settings
  const canManageParticipants = userRole === 'OWNER' || userRole === 'ADMIN';
  const canUpdateSettings = userRole === 'OWNER' || userRole === 'ADMIN';
  const canDeleteGroup = userRole === 'OWNER';

  useEffect(() => {
    if (isOpen) {
      setGroupName(conversation.name || '');
      setDescription(conversation.description || '');
      setIsPublic(conversation.isPublic || false);
      setMaxParticipants(conversation.maxParticipants || 100);
      setSelectedUsers([]);
      setError(null);
    }
  }, [isOpen, conversation]);

  const handleUserToggle = (userId: string) => {
    setSelectedUsers(prev => 
      prev.includes(userId) 
        ? prev.filter(id => id !== userId)
        : [...prev, userId]
    );
  };

  const handleSettingsUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!canUpdateSettings) {
      setError('You do not have permission to update group settings');
      return;
    }

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
      const updateRequest: UpdateGroupSettingsRequest = {
        name: groupName.trim(),
        description: description.trim() || null,
        isPublic,
        maxParticipants,
      };

      const response = await api.conversation.updateGroupSettings(conversation.id, updateRequest);
      
      if (onGroupUpdated) {
        onGroupUpdated(response);
      }

      onClose();
    } catch (error: any) {
      console.error('Failed to update group settings:', error);
      setError(error.message || 'Failed to update group settings');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddParticipants = async () => {
    if (!canManageParticipants) {
      setError('You do not have permission to manage participants');
      return;
    }

    if (selectedUsers.length === 0) {
      setError('Please select at least one user to add');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      for (const userId of selectedUsers) {
        await api.conversation.addParticipant(conversation.id, userId);
      }

      setSelectedUsers([]);
      
      if (onGroupUpdated) {
        // Refresh the conversation data
        const response = await api.get(`/conversations/${conversation.id}`);
        onGroupUpdated(response.data);
      }
    } catch (error: any) {
      console.error('Failed to add participants:', error);
      setError(error.message || 'Failed to add participants');
    } finally {
      setIsLoading(false);
    }
  };

  const handleRemoveParticipant = async (userId: string) => {
    if (!canManageParticipants) {
      setError('You do not have permission to manage participants');
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      await api.conversation.removeParticipant(conversation.id, userId);
      
      if (onGroupUpdated) {
        // Refresh the conversation data
        const response = await api.get(`/conversations/${conversation.id}`);
        onGroupUpdated(response.data);
      }
    } catch (error: any) {
      console.error('Failed to remove participant:', error);
      setError(error.message || 'Failed to remove participant');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteGroup = async () => {
    if (!canDeleteGroup) {
      setError('You do not have permission to delete this group');
      return;
    }

    if (!window.confirm('Are you sure you want to delete this group? This action cannot be undone.')) {
      return;
    }

    setError(null);
    setIsLoading(true);

    try {
      await api.conversation.deleteGroup(conversation.id);
      onGroupDeleted?.(); // Refresh the conversation list
      onClose();
    } catch (error: any) {
      console.error('Failed to delete group:', error);
      setError(error.message || 'Failed to delete group');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setGroupName(conversation.name || '');
    setDescription(conversation.description || '');
    setIsPublic(conversation.isPublic || false);
    setMaxParticipants(conversation.maxParticipants || 100);
    setSelectedUsers([]);
    setError(null);
    onClose();
  };

  if (!isOpen) return null;

  // Filter out users who are already participants
  // Handle both ConversationDto (with ConversationParticipant[]) and Conversation (with User[])
  const participantUserIds = conversation.participants?.map(p => {
    // Check if participant has nested user object or is a direct user
    if (p && typeof p === 'object' && 'user' in p && p.user) {
      return p.user.id;
    } else if (p && typeof p === 'object' && 'id' in p) {
      return p.id;
    }
    return null;
  }).filter(id => id !== null) || [];
  
  const availableUsers = users.filter(user => !participantUserIds.includes(user.id));

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-full max-w-2xl mx-4 max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold">Group Settings</h2>
            <div className="flex items-center space-x-2">
              <span className="text-sm text-gray-500">Your role: {userRole}</span>
              <button
                onClick={handleCancel}
                className="text-gray-500 hover:text-gray-700"
              >
                ✕
              </button>
            </div>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
              {error}
            </div>
          )}

          {/* Tabs */}
          <div className="flex border-b mb-6">
            <button
              onClick={() => setActiveTab('settings')}
              className={`px-4 py-2 font-medium ${
                activeTab === 'settings'
                  ? 'border-b-2 border-blue-500 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              Settings
            </button>
            <button
              onClick={() => setActiveTab('participants')}
              className={`px-4 py-2 font-medium ${
                activeTab === 'participants'
                  ? 'border-b-2 border-blue-500 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              Participants ({conversation.participants?.length || 0})
            </button>
          </div>

          {/* Settings Tab */}
          {activeTab === 'settings' && (
            <form onSubmit={handleSettingsUpdate} className="space-y-4">
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
                  disabled={!canUpdateSettings}
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
                  disabled={!canUpdateSettings}
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
                  disabled={!canUpdateSettings}
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
                  disabled={!canUpdateSettings}
                />
              </div>

              {/* Action Buttons */}
              <div className="flex justify-between pt-4">
                <div>
                  {canDeleteGroup && (
                    <button
                      type="button"
                      onClick={handleDeleteGroup}
                      className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500"
                      disabled={isLoading}
                    >
                      Delete Group
                    </button>
                  )}
                </div>
                <div className="flex space-x-3">
                  <button
                    type="button"
                    onClick={handleCancel}
                    className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-500"
                    disabled={isLoading}
                  >
                    Cancel
                  </button>
                  {canUpdateSettings && (
                    <button
                      type="submit"
                      disabled={isLoading || !groupName.trim()}
                      className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
                    >
                      {isLoading ? 'Updating...' : 'Update Settings'}
                    </button>
                  )}
                </div>
              </div>
            </form>
          )}

          {/* Participants Tab */}
          {activeTab === 'participants' && (
            <div className="space-y-6">
              {/* Current Participants */}
              <div>
                <h3 className="text-lg font-medium mb-3">Current Participants</h3>
                <div className="space-y-2">
                  {conversation.participants?.map((participant) => {
                    // Handle both ConversationDto (with ConversationParticipant[]) and Conversation (with User[])
                    const user = participant && typeof participant === 'object' && 'user' in participant && participant.user 
                      ? participant.user 
                      : participant as any;
                    const role = participant && typeof participant === 'object' && 'role' in participant 
                      ? participant.role 
                      : 'MEMBER';
                    
                    if (!user || !user.id) return null;
                    
                    return (
                      <div key={user.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-md">
                        <div className="flex items-center space-x-3">
                          <div className="flex-1">
                            <div className="text-sm font-medium">{user.displayName || user.username}</div>
                            <div className="text-xs text-gray-500">{user.email}</div>
                          </div>
                          <div className="flex items-center space-x-2">
                            <span className={`px-2 py-1 text-xs rounded-full ${
                              role === 'OWNER' ? 'bg-purple-100 text-purple-800' :
                              role === 'ADMIN' ? 'bg-blue-100 text-blue-800' :
                              'bg-gray-100 text-gray-800'
                            }`}>
                              {role}
                            </span>
                            {(user.isOnline || user.online) && (
                              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                            )}
                          </div>
                        </div>
                        {canManageParticipants && role !== 'OWNER' && (
                          <button
                            onClick={() => handleRemoveParticipant(user.id)}
                            className="text-red-600 hover:text-red-800 text-sm"
                            disabled={isLoading}
                          >
                            Remove
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Add Participants */}
              {canManageParticipants && availableUsers.length > 0 && (
                <div>
                  <h3 className="text-lg font-medium mb-3">Add Participants</h3>
                  <div className="max-h-40 overflow-y-auto border border-gray-300 rounded-md p-2 mb-3">
                    {availableUsers.map((user: User) => (
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
                    ))}
                  </div>
                  {selectedUsers.length > 0 && (
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-gray-600">
                        {selectedUsers.length} user{selectedUsers.length !== 1 ? 's' : ''} selected
                      </p>
                      <button
                        onClick={handleAddParticipants}
                        className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500"
                        disabled={isLoading}
                      >
                        {isLoading ? 'Adding...' : 'Add Selected'}
                      </button>
                    </div>
                  )}
                </div>
              )}

              {!canManageParticipants && (
                <div className="text-center py-4 text-gray-500">
                  You do not have permission to manage participants.
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
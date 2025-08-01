import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import PasswordChangeModal from './PasswordChangeModal';
import { api } from '../services/api';

interface UserSettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface CleanupReport {
  orphanedMessages: {
    totalMessages: number;
    orphanedMessagesCount: number;
    healthyMessagesCount: number;
  };
  orphanedParticipants: {
    totalParticipants: number;
    orphanedParticipantsCount: number;
    healthyParticipantsCount: number;
  };
  emptyConversations: {
    totalConversations: number;
    emptyConversationsCount: number;
  };
  finalStats: {
    totalUsers: number;
    totalConversations: number;
    totalParticipants: number;
    totalMessages: number;
  };
  dryRun: boolean;
  timestamp: string;
  deletionCounts?: {
    orphanedMessages: number;
    orphanedParticipants: number;
    emptyConversations: number;
  };
}

interface AdminStatus {
  isAdmin: boolean;
  userId: string;
}

const UserSettingsModal: React.FC<UserSettingsModalProps> = ({
  isOpen,
  onClose
}) => {
  const { user, updateUserProfile } = useAuth();
  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'preferences' | 'admin'>('profile');
  const [isPasswordChangeOpen, setIsPasswordChangeOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Admin state
  const [adminStatus, setAdminStatus] = useState<AdminStatus | null>(null);
  const [cleanupReport, setCleanupReport] = useState<CleanupReport | null>(null);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [confirmationText, setConfirmationText] = useState('');

  // Profile form state
  const [profileForm, setProfileForm] = useState({
    displayName: user?.displayName || '',
    email: user?.email || '',
    username: user?.username || ''
  });

  // Preferences form state
  const [preferences, setPreferences] = useState({
    theme: localStorage.getItem('theme') || 'auto'
  });

  // Check admin status when modal opens
  useEffect(() => {
    if (isOpen) {
      checkAdminStatus();
    }
  }, [isOpen]);

  const checkAdminStatus = async () => {
    try {
      console.log('🛡️ Checking admin status for user:', user?.username);
      const response = await api.get('/admin/status');
      console.log('🛡️ Admin status response:', response.data);
      setAdminStatus(response.data);
    } catch (error) {
      console.error('🛡️ Failed to check admin status:', error);
      setAdminStatus({ isAdmin: false, userId: user?.id || '' });
    }
  };

  const getCleanupPreview = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await api.get('/admin/cleanup/preview');
      const reportData = response.data.data || response.data;
      setCleanupReport(reportData);
      setSuccess('Cleanup preview generated successfully');
    } catch (error: any) {
      console.error('Failed to get cleanup preview:', error);
      setError(error.response?.data?.error || 'Failed to get cleanup preview');
    } finally {
      setIsLoading(false);
    }
  };

  const executeCleanup = async () => {
    if (confirmationText !== 'DELETE_ORPHANED_DATA') {
      setError('Please type the exact confirmation text');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await api.post('/admin/cleanup/execute', {
        confirmed: true,
        confirmationText: 'DELETE_ORPHANED_DATA'
      });
      
      setCleanupReport(response.data);
      setShowConfirmation(false);
      setConfirmationText('');
      setSuccess('Database cleanup completed successfully!');
      
    } catch (error: any) {
      console.error('Failed to execute cleanup:', error);
      setError(error.response?.data?.error || 'Failed to execute cleanup');
    } finally {
      setIsLoading(false);
    }
  };

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    setSuccess(null);

    try {
      // Only update fields that have changed
      const updates: any = {};
      if (profileForm.displayName !== user?.displayName) {
        updates.displayName = profileForm.displayName;
      }
      
      if (Object.keys(updates).length > 0) {
        await updateUserProfile(updates);
        setSuccess('Profile updated successfully!');
      } else {
        setSuccess('No changes to save.');
      }
    } catch (error: any) {
      console.error('Failed to update profile:', error);
      setError(error.response?.data?.message || 'Failed to update profile');
    } finally {
      setIsLoading(false);
    }
  };

  const handlePreferencesSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    setSuccess(null);

    try {
      // Save theme preference to localStorage
      localStorage.setItem('theme', preferences.theme);

      // Apply theme immediately
      if (preferences.theme === 'dark') {
        document.documentElement.classList.add('dark');
      } else if (preferences.theme === 'light') {
        document.documentElement.classList.remove('dark');
      } else {
        // Auto theme based on system preference
        if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
          document.documentElement.classList.add('dark');
        } else {
          document.documentElement.classList.remove('dark');
        }
      }

      setSuccess('Theme preferences saved successfully!');
    } catch (error: any) {
      console.error('Failed to save preferences:', error);
      setError('Failed to save preferences');
    } finally {
      setIsLoading(false);
    }
  };

  const clearMessages = () => {
    setError(null);
    setSuccess(null);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-800 rounded-lg max-w-2xl w-full mx-4 max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 flex items-center">
            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Settings
          </h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="flex h-[600px]">
          {/* Sidebar */}
          <div className="w-48 bg-gray-50 dark:bg-gray-900 border-r border-gray-200 dark:border-gray-700 p-4">
            <nav className="space-y-2">
              <button
                onClick={() => { setActiveTab('profile'); clearMessages(); }}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  activeTab === 'profile'
                    ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                }`}
              >
                <div className="flex items-center">
                  <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  Profile
                </div>
              </button>
              
              <button
                onClick={() => { setActiveTab('security'); clearMessages(); }}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  activeTab === 'security'
                    ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                }`}
              >
                <div className="flex items-center">
                  <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                  </svg>
                  Security
                </div>
              </button>
              
              <button
                onClick={() => { setActiveTab('preferences'); clearMessages(); }}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  activeTab === 'preferences'
                    ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                    : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                }`}
              >
                <div className="flex items-center">
                  <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4" />
                  </svg>
                  Preferences
                </div>
              </button>
              
              {(() => {
                console.log('🛡️ Admin tab render check:', { adminStatus, isAdmin: adminStatus?.isAdmin });
                return adminStatus?.isAdmin && (
                  <button
                    onClick={() => { setActiveTab('admin'); clearMessages(); }}
                    className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                      activeTab === 'admin'
                        ? 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800'
                    }`}
                  >
                    <div className="flex items-center">
                      <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                      Admin
                    </div>
                  </button>
                );
              })()}
            </nav>
          </div>

          {/* Content */}
          <div className="flex-1 p-6 overflow-y-auto">
            {/* Messages */}
            {error && (
              <div className="bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-700 rounded-lg p-3 mb-4">
                <p className="text-red-800 dark:text-red-200 text-sm">{error}</p>
              </div>
            )}
            
            {success && (
              <div className="bg-green-50 dark:bg-green-900 border border-green-200 dark:border-green-700 rounded-lg p-3 mb-4">
                <p className="text-green-800 dark:text-green-200 text-sm">{success}</p>
              </div>
            )}

            {/* Profile Tab */}
            {activeTab === 'profile' && (
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Profile Information</h3>
                <form onSubmit={handleProfileSubmit} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Display Name
                    </label>
                    <input
                      type="text"
                      value={profileForm.displayName}
                      onChange={(e) => setProfileForm(prev => ({ ...prev, displayName: e.target.value }))}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                      placeholder="Enter your display name"
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Username
                    </label>
                    <input
                      type="text"
                      value={profileForm.username}
                      disabled
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-100 dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed"
                    />
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Username cannot be changed</p>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Email
                    </label>
                    <input
                      type="email"
                      value={profileForm.email}
                      disabled
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-100 dark:bg-gray-600 text-gray-500 dark:text-gray-400 cursor-not-allowed"
                    />
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Email cannot be changed</p>
                  </div>
                  
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isLoading ? 'Saving...' : 'Save Changes'}
                  </button>
                </form>
              </div>
            )}

            {/* Security Tab */}
            {activeTab === 'security' && (
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Security Settings</h3>
                <div className="space-y-4">
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                    <h4 className="font-medium text-gray-900 dark:text-gray-100 mb-2">Password</h4>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
                      Keep your account secure with a strong password
                    </p>
                    <button
                      onClick={() => setIsPasswordChangeOpen(true)}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                      Change Password
                    </button>
                  </div>
                  
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                    <h4 className="font-medium text-gray-900 dark:text-gray-100 mb-2">Account Information</h4>
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span className="text-gray-600 dark:text-gray-400">Account created:</span>
                        <span className="text-gray-900 dark:text-gray-100">
                          {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Unknown'}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-600 dark:text-gray-400">Last login:</span>
                        <span className="text-gray-900 dark:text-gray-100">
                          {user?.lastSeenAt ? new Date(user.lastSeenAt).toLocaleDateString() : 'Unknown'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Preferences Tab */}
            {activeTab === 'preferences' && (
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Preferences</h3>
                <form onSubmit={handlePreferencesSubmit} className="space-y-6">
                  {/* Theme */}
                  <div>
                    <h4 className="font-medium text-gray-900 dark:text-gray-100 mb-3">Appearance</h4>
                    <div className="space-y-2">
                      <label className="flex items-center">
                        <input
                          type="radio"
                          name="theme"
                          value="light"
                          checked={preferences.theme === 'light'}
                          onChange={(e) => setPreferences(prev => ({ ...prev, theme: e.target.value }))}
                          className="mr-2"
                        />
                        <span className="text-sm text-gray-700 dark:text-gray-300">Light theme</span>
                      </label>
                      <label className="flex items-center">
                        <input
                          type="radio"
                          name="theme"
                          value="dark"
                          checked={preferences.theme === 'dark'}
                          onChange={(e) => setPreferences(prev => ({ ...prev, theme: e.target.value }))}
                          className="mr-2"
                        />
                        <span className="text-sm text-gray-700 dark:text-gray-300">Dark theme</span>
                      </label>
                      <label className="flex items-center">
                        <input
                          type="radio"
                          name="theme"
                          value="auto"
                          checked={preferences.theme === 'auto'}
                          onChange={(e) => setPreferences(prev => ({ ...prev, theme: e.target.value }))}
                          className="mr-2"
                        />
                        <span className="text-sm text-gray-700 dark:text-gray-300">Auto (system preference)</span>
                      </label>
                    </div>
                  </div>


                  <button
                    type="submit"
                    disabled={isLoading}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isLoading ? 'Saving...' : 'Save Preferences'}
                  </button>
                </form>
              </div>
            )}

            {/* Admin Tab */}
            {activeTab === 'admin' && adminStatus?.isAdmin && (
              <div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Database Administration</h3>
                
                <div className="space-y-6">
                  <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg p-4">
                    <div className="flex items-start">
                      <svg className="w-5 h-5 text-yellow-600 dark:text-yellow-400 mt-0.5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 15.5c-.77.833.192 2.5 1.732 2.5z" />
                      </svg>
                      <div>
                        <h4 className="font-medium text-yellow-800 dark:text-yellow-200 mb-1">Admin Access</h4>
                        <p className="text-sm text-yellow-700 dark:text-yellow-300">
                          You have administrative privileges. Use these tools carefully as they affect the entire database.
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-lg p-6">
                    <h4 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-4">Database Cleanup</h4>
                    <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
                      Clean up orphaned data in the database including messages without conversations, 
                      participants without users, and empty conversations.
                    </p>
                    
                    <div className="flex space-x-3 mb-4">
                      <button
                        onClick={getCleanupPreview}
                        disabled={isLoading}
                        className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {isLoading ? 'Analyzing...' : '🔍 Analyze Database'}
                      </button>
                    </div>

                    {cleanupReport && (
                      <div className="mt-6 space-y-4">
                        <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4">
                          <h5 className="font-medium text-gray-900 dark:text-gray-100 mb-3">Cleanup Report</h5>
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                            <div>
                              <div className="font-medium text-gray-700 dark:text-gray-300">Orphaned Messages</div>
                              <div className="text-gray-600 dark:text-gray-400">
                                {cleanupReport.orphanedMessages.orphanedMessagesCount} of {cleanupReport.orphanedMessages.totalMessages} total
                              </div>
                            </div>
                            <div>
                              <div className="font-medium text-gray-700 dark:text-gray-300">Orphaned Participants</div>
                              <div className="text-gray-600 dark:text-gray-400">
                                {cleanupReport.orphanedParticipants.orphanedParticipantsCount} of {cleanupReport.orphanedParticipants.totalParticipants} total
                              </div>
                            </div>
                            <div>
                              <div className="font-medium text-gray-700 dark:text-gray-300">Empty Conversations</div>
                              <div className="text-gray-600 dark:text-gray-400">
                                {cleanupReport.emptyConversations.emptyConversationsCount} of {cleanupReport.emptyConversations.totalConversations} total
                              </div>
                            </div>
                            <div>
                              <div className="font-medium text-gray-700 dark:text-gray-300">Database Health</div>
                              <div className="text-gray-600 dark:text-gray-400">
                                {cleanupReport.finalStats.totalUsers} users, {cleanupReport.finalStats.totalMessages} messages
                              </div>
                            </div>
                          </div>
                          
                          {cleanupReport.deletionCounts && (
                            <div className="mt-4 p-3 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-700 rounded">
                              <div className="font-medium text-green-800 dark:text-green-200 mb-2">✅ Cleanup Completed</div>
                              <div className="text-sm text-green-700 dark:text-green-300">
                                Deleted: {cleanupReport.deletionCounts.orphanedMessages} messages, 
                                {cleanupReport.deletionCounts.orphanedParticipants} participants, 
                                {cleanupReport.deletionCounts.emptyConversations} conversations
                              </div>
                            </div>
                          )}
                        </div>

                        {!cleanupReport.deletionCounts && (
                          <button
                            onClick={() => setShowConfirmation(true)}
                            disabled={isLoading}
                            className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                          >
                            🗑️ Execute Cleanup
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Confirmation Modal */}
                {showConfirmation && (
                  <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]">
                    <div className="bg-white dark:bg-gray-800 rounded-lg max-w-md w-full mx-4 p-6">
                      <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
                        ⚠️ Confirm Database Cleanup
                      </h3>
                      <p className="text-sm text-gray-600 dark:text-gray-300 mb-4">
                        This action will permanently delete orphaned data from the database. 
                        This cannot be undone.
                      </p>
                      <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mb-4">
                        Type <code className="bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded">DELETE_ORPHANED_DATA</code> to confirm:
                      </p>
                      <input
                        type="text"
                        value={confirmationText}
                        onChange={(e) => setConfirmationText(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg mb-4 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                        placeholder="DELETE_ORPHANED_DATA"
                      />
                      <div className="flex justify-end space-x-3">
                        <button
                          onClick={() => {
                            setShowConfirmation(false);
                            setConfirmationText('');
                          }}
                          className="px-4 py-2 text-gray-700 dark:text-gray-300 bg-gray-200 dark:bg-gray-600 rounded-lg hover:bg-gray-300 dark:hover:bg-gray-500"
                        >
                          Cancel
                        </button>
                        <button
                          onClick={executeCleanup}
                          disabled={isLoading || confirmationText !== 'DELETE_ORPHANED_DATA'}
                          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          {isLoading ? 'Deleting...' : 'Delete Data'}
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Password Change Modal */}
      <PasswordChangeModal
        isOpen={isPasswordChangeOpen}
        onClose={() => setIsPasswordChangeOpen(false)}
        onSuccess={() => {
          setIsPasswordChangeOpen(false);
          setSuccess('Password changed successfully!');
        }}
      />
    </div>
  );
};

export default UserSettingsModal;
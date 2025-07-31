import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

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

const AdminCleanupPanel: React.FC = () => {
  const [adminStatus, setAdminStatus] = useState<AdminStatus | null>(null);
  const [cleanupReport, setCleanupReport] = useState<CleanupReport | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [confirmationText, setConfirmationText] = useState('');

  // Check admin status on component mount
  useEffect(() => {
    checkAdminStatus();
  }, []);

  const checkAdminStatus = async () => {
    try {
      const response = await api.get('/api/admin/status');
      setAdminStatus(response.data);
    } catch (error) {
      console.error('Failed to check admin status:', error);
      setError('Failed to verify admin status');
    }
  };

  const getCleanupPreview = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await api.get('/api/admin/cleanup/preview');
      setCleanupReport(response.data);
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
      const response = await api.post('/api/admin/cleanup/execute', {
        confirmed: true,
        confirmationText: 'DELETE_ORPHANED_DATA'
      });
      
      setCleanupReport(response.data);
      setShowConfirmation(false);
      setConfirmationText('');
      
    } catch (error: any) {
      console.error('Failed to execute cleanup:', error);
      setError(error.response?.data?.error || 'Failed to execute cleanup');
    } finally {
      setIsLoading(false);
    }
  };

  // Don't render if not admin
  if (!adminStatus?.isAdmin) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-700 rounded-lg p-4">
          <h2 className="text-lg font-semibold text-red-800 dark:text-red-200 mb-2">
            Access Denied
          </h2>
          <p className="text-red-600 dark:text-red-300">
            Admin privileges required to access database cleanup tools.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg">
        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            🛡️ Admin Database Cleanup
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-2">
            Safely remove orphaned data from PostgreSQL and MongoDB
          </p>
          <div className="mt-2 text-xs text-blue-600 dark:text-blue-400">
            Admin: {adminStatus.userId}
          </div>
        </div>

        <div className="p-6">
          {/* Warning Banner */}
          <div className="bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-700 rounded-lg p-4 mb-6">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800 dark:text-red-200">
                  Critical Operation Warning
                </h3>
                <p className="mt-1 text-sm text-red-700 dark:text-red-300">
                  This tool permanently deletes orphaned data. Always preview first and backup your database before executing cleanup.
                </p>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-4 mb-6">
            <button
              onClick={getCleanupPreview}
              disabled={isLoading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Loading...' : '🔍 Preview Cleanup'}
            </button>
            
            {cleanupReport && !cleanupReport.dryRun && (
              <button
                onClick={() => setShowConfirmation(true)}
                disabled={isLoading}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                🗑️ Execute Cleanup
              </button>
            )}
          </div>

          {/* Error Display */}
          {error && (
            <div className="bg-red-50 dark:bg-red-900 border border-red-200 dark:border-red-700 rounded-lg p-4 mb-6">
              <p className="text-red-800 dark:text-red-200">{error}</p>
            </div>
          )}

          {/* Cleanup Report */}
          {cleanupReport && (
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                  Cleanup Report
                </h2>
                <div className="text-sm text-gray-500 dark:text-gray-400">
                  {cleanupReport.dryRun ? '🧪 Preview Mode' : '⚠️ Executed'} - {new Date(cleanupReport.timestamp).toLocaleString()}
                </div>
              </div>

              {/* Database Stats */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                  <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                    {cleanupReport.finalStats.totalUsers}
                  </div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">Users</div>
                </div>
                <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                  <div className="text-2xl font-bold text-green-600 dark:text-green-400">
                    {cleanupReport.finalStats.totalConversations}
                  </div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">Conversations</div>
                </div>
                <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                  <div className="text-2xl font-bold text-purple-600 dark:text-purple-400">
                    {cleanupReport.finalStats.totalParticipants}
                  </div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">Participants</div>
                </div>
                <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-lg">
                  <div className="text-2xl font-bold text-orange-600 dark:text-orange-400">
                    {cleanupReport.finalStats.totalMessages}
                  </div>
                  <div className="text-sm text-gray-600 dark:text-gray-400">Messages</div>
                </div>
              </div>

              {/* Cleanup Details */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Orphaned Messages */}
                <div className="bg-red-50 dark:bg-red-900 p-4 rounded-lg border border-red-200 dark:border-red-700">
                  <h3 className="font-semibold text-red-800 dark:text-red-200 mb-2">
                    📨 Orphaned Messages
                  </h3>
                  <div className="space-y-1 text-sm">
                    <div className="text-red-700 dark:text-red-300">
                      Orphaned: <span className="font-bold">{cleanupReport.orphanedMessages.orphanedMessagesCount}</span>
                    </div>
                    <div className="text-green-700 dark:text-green-300">
                      Healthy: <span className="font-bold">{cleanupReport.orphanedMessages.healthyMessagesCount}</span>
                    </div>
                    {cleanupReport.deletionCounts && (
                      <div className="text-red-800 dark:text-red-200 font-bold">
                        ✅ Deleted: {cleanupReport.deletionCounts.orphanedMessages}
                      </div>
                    )}
                  </div>
                </div>

                {/* Orphaned Participants */}
                <div className="bg-yellow-50 dark:bg-yellow-900 p-4 rounded-lg border border-yellow-200 dark:border-yellow-700">
                  <h3 className="font-semibold text-yellow-800 dark:text-yellow-200 mb-2">
                    👥 Orphaned Participants
                  </h3>
                  <div className="space-y-1 text-sm">
                    <div className="text-yellow-700 dark:text-yellow-300">
                      Orphaned: <span className="font-bold">{cleanupReport.orphanedParticipants.orphanedParticipantsCount}</span>
                    </div>
                    <div className="text-green-700 dark:text-green-300">
                      Healthy: <span className="font-bold">{cleanupReport.orphanedParticipants.healthyParticipantsCount}</span>
                    </div>
                    {cleanupReport.deletionCounts && (
                      <div className="text-yellow-800 dark:text-yellow-200 font-bold">
                        ✅ Deleted: {cleanupReport.deletionCounts.orphanedParticipants}
                      </div>
                    )}
                  </div>
                </div>

                {/* Empty Conversations */}
                <div className="bg-purple-50 dark:bg-purple-900 p-4 rounded-lg border border-purple-200 dark:border-purple-700">
                  <h3 className="font-semibold text-purple-800 dark:text-purple-200 mb-2">
                    💬 Empty Conversations
                  </h3>
                  <div className="space-y-1 text-sm">
                    <div className="text-purple-700 dark:text-purple-300">
                      Empty: <span className="font-bold">{cleanupReport.emptyConversations.emptyConversationsCount}</span>
                    </div>
                    <div className="text-green-700 dark:text-green-300">
                      Total: <span className="font-bold">{cleanupReport.emptyConversations.totalConversations}</span>
                    </div>
                    {cleanupReport.deletionCounts && (
                      <div className="text-purple-800 dark:text-purple-200 font-bold">
                        ✅ Deleted: {cleanupReport.deletionCounts.emptyConversations}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Confirmation Modal */}
          {showConfirmation && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white dark:bg-gray-800 p-6 rounded-lg max-w-md w-full mx-4">
                <h3 className="text-lg font-semibold text-red-600 dark:text-red-400 mb-4">
                  ⚠️ Confirm Database Cleanup
                </h3>
                <p className="text-gray-700 dark:text-gray-300 mb-4">
                  This will permanently delete orphaned data. Type <code className="bg-gray-200 dark:bg-gray-700 px-1 rounded">DELETE_ORPHANED_DATA</code> to confirm:
                </p>
                <input
                  type="text"
                  value={confirmationText}
                  onChange={(e) => setConfirmationText(e.target.value)}
                  className="w-full p-2 border border-gray-300 dark:border-gray-600 rounded mb-4 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                  placeholder="Type confirmation text"
                />
                <div className="flex gap-2">
                  <button
                    onClick={executeCleanup}
                    disabled={confirmationText !== 'DELETE_ORPHANED_DATA' || isLoading}
                    className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isLoading ? 'Executing...' : 'Execute Cleanup'}
                  </button>
                  <button
                    onClick={() => {
                      setShowConfirmation(false);
                      setConfirmationText('');
                    }}
                    className="px-4 py-2 bg-gray-300 dark:bg-gray-600 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-400 dark:hover:bg-gray-500"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminCleanupPanel;
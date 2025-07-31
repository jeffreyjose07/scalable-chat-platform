import React from 'react';
import { Link } from 'react-router-dom';
import AdminCleanupPanel from '../components/AdminCleanupPanel';
import ThemeToggle from '../components/ThemeToggle';

const AdminPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Header */}
      <header className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-4">
              <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                🛡️ Admin Panel
              </h1>
              <Link 
                to="/chat" 
                className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-300 text-sm"
              >
                ← Back to Chat
              </Link>
            </div>
            <div className="flex items-center space-x-4">
              <ThemeToggle />
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="py-6">
        <AdminCleanupPanel />
      </main>

      {/* Footer */}
      <footer className="mt-12 border-t border-gray-200 dark:border-gray-700 py-6">
        <div className="max-w-6xl mx-auto px-6 text-center text-sm text-gray-500 dark:text-gray-400">
          <p>⚠️ Admin tools - Use with extreme caution</p>
        </div>
      </footer>
    </div>
  );
};

export default AdminPage;
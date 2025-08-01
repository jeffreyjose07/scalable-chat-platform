import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';

import { AuthProvider, useAuth } from './hooks/useAuth';
import { WebSocketProvider } from './hooks/useWebSocket';
import { ThemeProvider } from './contexts/ThemeContext';
import LoginPage from './pages/LoginPage';
import ChatPage from './pages/ChatPage';
import AdminPage from './pages/AdminPage';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorBoundary, { ChatErrorBoundary } from './components/ErrorBoundary';
import SecurityNotification from './components/SecurityNotification';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 3,
    },
  },
});

// Component to handle root route redirection
const RootRedirect: React.FC = () => {
  const { user, isLoading, token } = useAuth();

  console.log('🏠 RootRedirect rendered at URL:', window.location.href);
  console.log('🏠 Auth State:', { 
    isLoading, 
    hasUser: !!user, 
    hasToken: !!token,
    userDetails: user ? { id: user.id, username: user.username } : null 
  });

  if (isLoading) {
    console.log('🏠 RootRedirect: Showing loading spinner...');
    return (
      <div className="flex items-center justify-center h-screen bg-gray-50 dark:bg-gray-900">
        <div className="flex flex-col items-center space-y-4">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-500"></div>
          <div className="text-gray-600 dark:text-gray-400">Loading...</div>
        </div>
      </div>
    );
  }

  const redirect = user ? '/chat' : '/login';
  console.log('🏠 RootRedirect: About to redirect to:', redirect);
  console.log('🏠 Current URL before redirect:', window.location.href);
  
  return user ? <Navigate to="/chat" replace /> : <Navigate to="/login" replace />;
};

const AppContent: React.FC = () => {
  const { securityLogout, dismissSecurityNotification } = useAuth();

  return (
    <ErrorBoundary>
      <WebSocketProvider>
        <Router>
          <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route 
                path="/chat" 
                element={
                  <ProtectedRoute>
                    <ChatErrorBoundary>
                      <ChatPage />
                    </ChatErrorBoundary>
                  </ProtectedRoute>
                } 
              />
              <Route 
                path="/admin" 
                element={
                  <ProtectedRoute>
                    <AdminPage />
                  </ProtectedRoute>
                } 
              />
              <Route path="/" element={<RootRedirect />} />
            </Routes>
            <Toaster position="top-right" />
            {securityLogout && (
              <SecurityNotification onDismiss={dismissSecurityNotification} />
            )}
          </div>
        </Router>
      </WebSocketProvider>
    </ErrorBoundary>
  );
};

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <ErrorBoundary>
            <AuthProvider>
              <AppContent />
            </AuthProvider>
          </ErrorBoundary>
        </ThemeProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
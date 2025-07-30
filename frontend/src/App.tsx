import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';

import { AuthProvider } from './hooks/useAuth';
import { WebSocketProvider } from './hooks/useWebSocket';
import { ThemeProvider } from './contexts/ThemeContext';
import LoginPage from './pages/LoginPage';
import ChatPage from './pages/ChatPage';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorBoundary, { ChatErrorBoundary } from './components/ErrorBoundary';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 3,
    },
  },
});

function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <ErrorBoundary>
            <AuthProvider>
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
                        <Route path="/" element={<LoginPage />} />
                      </Routes>
                      <Toaster position="top-right" />
                    </div>
                  </Router>
                </WebSocketProvider>
              </ErrorBoundary>
            </AuthProvider>
          </ErrorBoundary>
        </ThemeProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

export default App;
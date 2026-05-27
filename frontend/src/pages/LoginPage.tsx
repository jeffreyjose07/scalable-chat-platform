import React, { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import axios from 'axios';
import toast from 'react-hot-toast';
import { getApiBaseUrl } from '../utils/networkUtils';

const LoginPage: React.FC = () => {
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [showForgotPassword, setShowForgotPassword] = useState(false);
  const [forgotPasswordEmail, setForgotPasswordEmail] = useState('');
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    displayName: ''
  });
  const [isLoading, setIsLoading] = useState(false);
  const [isForgotPasswordLoading, setIsForgotPasswordLoading] = useState(false);
  const { login, register, user } = useAuth();
  const apiUrl = getApiBaseUrl();

  console.log('🔑 LoginPage: Checking auth state:', {
    hasUser: !!user,
    userDetails: user ? { id: user.id, username: user.username } : null,
    currentURL: window.location.href
  });

  if (user) {
    console.log('🔑 LoginPage: User authenticated, redirecting to /chat');
    return <Navigate to="/chat" replace />;
  }

  console.log('🔑 LoginPage: No user, showing login form');

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      if (isRegisterMode) {
        await register(formData.username, formData.email, formData.password, formData.displayName);
      } else {
        await login(formData.email, formData.password);
      }
    } catch (error) {
      // Error handled by auth functions
    } finally {
      setIsLoading(false);
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsForgotPasswordLoading(true);
    try {
      await axios.post(`${apiUrl}/api/auth/forgot-password`, { email: forgotPasswordEmail });
      toast.success('Password reset email sent if account exists');
      setShowForgotPassword(false);
      setForgotPasswordEmail('');
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to send reset email';
      toast.error(errorMessage);
    } finally {
      setIsForgotPasswordLoading(false);
    }
  };

  const inputClass = `
    relative block w-full px-4 py-3 min-h-[44px]
    bg-white/5 border border-white/10 rounded-xl
    text-gray-100 placeholder-zinc-500
    focus:outline-none focus:border-green-600/70 focus:ring-2 focus:ring-green-600/20
    transition-all duration-200 text-sm
  `;

  return (
    <div
      className="min-h-screen flex items-center justify-center relative overflow-hidden py-12 px-4"
      style={{ background: '#09090B' }}
    >
      {/* Atmospheric dot grid */}
      <div className="absolute inset-0 login-dot-grid" />

      {/* Ambient glows */}
      <div
        className="absolute top-1/4 left-1/3 w-96 h-96 rounded-full pointer-events-none"
        style={{ background: 'radial-gradient(circle, rgba(180,83,9,0.12) 0%, transparent 70%)' }}
      />
      <div
        className="absolute bottom-1/4 right-1/3 w-64 h-64 rounded-full pointer-events-none"
        style={{ background: 'radial-gradient(circle, rgba(180,83,9,0.08) 0%, transparent 70%)' }}
      />

      {/* Card */}
      <div className="relative z-10 w-full max-w-md animate-fadeIn">

        {/* Brand mark */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-green-600 mb-5 shadow-lg shadow-green-900/40">
            <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path d="M2 5a2 2 0 012-2h7a2 2 0 012 2v4a2 2 0 01-2 2H9l-3 3v-3H4a2 2 0 01-2-2V5z" />
              <path d="M15 7v2a4 4 0 01-4 4H9.828l-1.766 1.767c.28.149.599.233.938.233h2l3 3v-3h2a2 2 0 002-2V9a2 2 0 00-2-2h-1z" />
            </svg>
          </div>
          <h1 className="font-display text-3xl font-semibold text-white tracking-tight">
            Chat Platform
          </h1>
          <p className="mt-2 text-sm text-zinc-500">
            {isRegisterMode ? 'Create your account to get started' : 'Welcome back — sign in to continue'}
          </p>
        </div>

        {/* Form card */}
        <div
          className="rounded-2xl p-7 border border-white/8"
          style={{ background: 'rgba(255,255,255,0.04)', backdropFilter: 'blur(12px)' }}
        >
          <form className="space-y-4" onSubmit={handleSubmit}>
            {isRegisterMode && (
              <>
                <div>
                  <label htmlFor="username" className="sr-only">Username</label>
                  <input
                    id="username"
                    name="username"
                    type="text"
                    required
                    className={inputClass}
                    placeholder="Username"
                    value={formData.username}
                    onChange={handleInputChange}
                  />
                </div>
                <div>
                  <label htmlFor="displayName" className="sr-only">Display Name</label>
                  <input
                    id="displayName"
                    name="displayName"
                    type="text"
                    required
                    className={inputClass}
                    placeholder="Display Name"
                    value={formData.displayName}
                    onChange={handleInputChange}
                  />
                </div>
              </>
            )}

            <div>
              <label htmlFor="email" className="sr-only">Email address</label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                className={inputClass}
                placeholder="Email address"
                value={formData.email}
                onChange={handleInputChange}
              />
            </div>

            <div>
              <label htmlFor="password" className="sr-only">Password</label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete={isRegisterMode ? 'new-password' : 'current-password'}
                required
                className={inputClass}
                placeholder={isRegisterMode ? 'Password (minimum 6 characters)' : 'Password'}
                value={formData.password}
                onChange={handleInputChange}
                minLength={6}
              />
            </div>

            <div className="space-y-3 pt-1">
              <button
                type="submit"
                disabled={isLoading}
                className="w-full flex justify-center items-center py-3 px-4 min-h-[44px] rounded-xl text-sm font-semibold text-white bg-green-600 hover:bg-green-500 focus:outline-none focus:ring-2 focus:ring-green-600/50 focus:ring-offset-2 focus:ring-offset-transparent disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 shadow-lg shadow-green-900/30 hover:shadow-green-900/50"
              >
                {isLoading
                  ? (isRegisterMode ? 'Creating Account…' : 'Signing in…')
                  : (isRegisterMode ? 'Create Account' : 'Sign In')
                }
              </button>

              <div className="text-center space-y-2">
                <button
                  type="button"
                  onClick={() => setIsRegisterMode(!isRegisterMode)}
                  className="text-zinc-400 hover:text-green-400 text-sm transition-colors"
                >
                  {isRegisterMode
                    ? 'Already have an account? Sign in'
                    : "Don't have an account? Create one"
                  }
                </button>

                {!isRegisterMode && (
                  <div>
                    <button
                      type="button"
                      onClick={() => setShowForgotPassword(true)}
                      className="text-zinc-500 hover:text-zinc-300 text-sm transition-colors"
                    >
                      Forgot your password?
                    </button>
                  </div>
                )}
              </div>
            </div>
          </form>
        </div>

        {/* Footer credit */}
        <p className="text-center text-xs text-zinc-600 mt-6">
          Built by{' '}
          <a
            href="https://jeffreyjose07.github.io"
            target="_blank"
            rel="noopener noreferrer"
            className="text-zinc-500 hover:text-green-500 transition-colors"
          >
            Jeffrey Jose
          </a>
        </p>
      </div>

      {/* Forgot Password Modal */}
      {showForgotPassword && (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4" style={{ background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(4px)' }}>
          <div
            className="w-full max-w-sm rounded-2xl p-6 border border-white/10 animate-fadeIn"
            style={{ background: 'rgba(18,18,20,0.95)', backdropFilter: 'blur(16px)' }}
          >
            <h3 className="font-display text-xl font-semibold text-white mb-1">Reset Password</h3>
            <p className="text-sm text-zinc-500 mb-5">We'll send a reset link to your email.</p>

            <form onSubmit={handleForgotPassword} className="space-y-4">
              <div>
                <label htmlFor="forgotEmail" className="block text-xs font-medium text-zinc-400 mb-1.5 uppercase tracking-wide">
                  Email Address
                </label>
                <input
                  id="forgotEmail"
                  type="email"
                  required
                  className={inputClass}
                  placeholder="you@example.com"
                  value={forgotPasswordEmail}
                  onChange={(e) => setForgotPasswordEmail(e.target.value)}
                />
              </div>

              <div className="flex space-x-3 pt-1">
                <button
                  type="button"
                  onClick={() => { setShowForgotPassword(false); setForgotPasswordEmail(''); }}
                  className="flex-1 px-4 py-2.5 text-sm font-medium text-zinc-300 bg-white/5 border border-white/10 rounded-xl hover:bg-white/10 transition-colors min-h-[44px]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isForgotPasswordLoading}
                  className="flex-1 px-4 py-2.5 text-sm font-semibold text-white bg-green-600 hover:bg-green-500 rounded-xl disabled:opacity-50 transition-all duration-200 min-h-[44px]"
                >
                  {isForgotPasswordLoading ? 'Sending…' : 'Send Link'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default LoginPage;

import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';
import { getApiBaseUrl } from '../utils/networkUtils';
import { tokenStorage } from '../utils/secureStorage';

interface User {
  id: string;
  username: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
}

interface MessageResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface AuthResponse {
  token: string;
  type: string;
  user: {
    id: string;
    username: string;
    email: string;
    displayName: string;
    createdAt: string;
  };
  issuedAt: string;
  success: boolean;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(tokenStorage.get());
  const [isLoading, setIsLoading] = useState(true);

  const apiUrl = getApiBaseUrl();

  useEffect(() => {
    if (token) {
      axios.get(`${apiUrl}/api/auth/me`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      .then(response => {
        const messageResponse = response.data as MessageResponse<User>;
        if (messageResponse.success && messageResponse.data) {
          // Map backend User to frontend User interface
          const user: User = {
            id: messageResponse.data.id,
            username: messageResponse.data.username,
            email: messageResponse.data.email,
            displayName: messageResponse.data.displayName,
            avatarUrl: undefined // Backend doesn't have avatarUrl yet
          };
          setUser(user);
        } else {
          tokenStorage.remove();
          setToken(null);
        }
      })
      .catch(() => {
        tokenStorage.remove();
        setToken(null);
      })
      .finally(() => {
        setIsLoading(false);
      });
    } else {
      setIsLoading(false);
    }
  }, [token, apiUrl]);

  const login = async (email: string, password: string) => {
    try {
      const response = await axios.post(`${apiUrl}/api/auth/login`, {
        email,
        password
      });
      
      const messageResponse = response.data as MessageResponse<AuthResponse>;
      
      if (messageResponse.success && messageResponse.data) {
        const { token: newToken, user: userData } = messageResponse.data;
        
        // Map backend UserInfo to frontend User interface
        const user: User = {
          id: userData.id,
          username: userData.username,
          email: userData.email,
          displayName: userData.displayName,
          avatarUrl: undefined // Backend doesn't have avatarUrl yet
        };
        
        // Store token securely (sessionStorage for security, localStorage for persistence if needed)
        tokenStorage.set(newToken, false); // Don't persist by default for security
        setToken(newToken);
        setUser(user);
        
        toast.success(messageResponse.message || 'Login successful');
      } else {
        toast.error(messageResponse.message || 'Login failed');
        throw new Error(messageResponse.message || 'Login failed');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Login failed';
      toast.error(errorMessage);
      throw error;
    }
  };

  const register = async (username: string, email: string, password: string, displayName: string) => {
    try {
      const response = await axios.post(`${apiUrl}/api/auth/register`, {
        username,
        email,
        password,
        displayName
      });
      
      const messageResponse = response.data as MessageResponse<AuthResponse>;
      
      if (messageResponse.success && messageResponse.data) {
        const { token: newToken, user: userData } = messageResponse.data;
        
        // Map backend UserInfo to frontend User interface
        const user: User = {
          id: userData.id,
          username: userData.username,
          email: userData.email,
          displayName: userData.displayName,
          avatarUrl: undefined // Backend doesn't have avatarUrl yet
        };
        
        // Store token securely (sessionStorage for security, localStorage for persistence if needed)
        tokenStorage.set(newToken, false); // Don't persist by default for security
        setToken(newToken);
        setUser(user);
        
        toast.success(messageResponse.message || 'Registration successful');
      } else {
        toast.error(messageResponse.message || 'Registration failed');
        throw new Error(messageResponse.message || 'Registration failed');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Registration failed';
      toast.error(errorMessage);
      throw error;
    }
  };

  const logout = async () => {
    try {
      if (token) {
        const response = await axios.post(`${apiUrl}/api/auth/logout`, {}, {
          headers: { Authorization: `Bearer ${token}` }
        });
        const messageResponse = response.data as MessageResponse<void>;
        if (messageResponse.success) {
          toast.success(messageResponse.message || 'Logged out successfully');
        }
      }
    } catch (error) {
      console.warn('Logout request failed:', error);
    } finally {
      // Securely remove token from all storage locations
      tokenStorage.remove();
      setToken(null);
      setUser(null);
      
      // Clear all cached data on logout for security and fresh start
      try {
        sessionStorage.removeItem('cached_conversations');
        sessionStorage.removeItem('recent_messages');
        console.log('🧹 Cleared all cached data on logout');
      } catch (error) {
        console.warn('Failed to clear cache on logout:', error);
      }
      
      if (!token) {
        toast.success('Logged out successfully');
      }
    }
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
};
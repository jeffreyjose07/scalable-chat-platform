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
  createdAt?: string;
  lastSeenAt?: string;
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
  updateUserProfile: (updates: Partial<User>) => Promise<void>;
  isLoading: boolean;
  securityLogout: boolean;
  dismissSecurityNotification: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

// Initialize token synchronously to prevent race conditions
const initializeAuth = () => {
  const initialToken = tokenStorage.get();
  const wasRemoved = tokenStorage.wasTokenRemoved();
  
  console.log('🔐 AuthProvider initializing synchronously...');
  console.log('🔐 Retrieved token from storage:', initialToken ? 'TOKEN_EXISTS' : 'NO_TOKEN');
  
  return {
    token: initialToken,
    securityLogout: initialToken === null && wasRemoved,
    isLoading: !!initialToken // Only show loading if we have a token to validate
  };
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const initialAuthState = initializeAuth();
  
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(initialAuthState.token);
  const [isLoading, setIsLoading] = useState(initialAuthState.isLoading);
  const [securityLogout, setSecurityLogout] = useState(initialAuthState.securityLogout);

  const apiUrl = getApiBaseUrl();

  // Log the initial state
  useEffect(() => {
    console.log('🔐 AuthProvider initialized with:', {
      hasToken: !!token,
      isLoading,
      securityLogout
    });
  }, []);

  useEffect(() => {
    console.log('🔐 Token effect triggered, token exists:', !!token);
    if (token) {
      console.log('🔐 Validating token with /api/auth/me...');
      axios.get(`${apiUrl}/api/auth/me`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      .then(response => {
        console.log('🔐 /api/auth/me success:', response.data);
        const messageResponse = response.data as MessageResponse<User>;
        if (messageResponse.success && messageResponse.data) {
          // Map backend User to frontend User interface
          const user: User = {
            id: messageResponse.data.id,
            username: messageResponse.data.username,
            email: messageResponse.data.email,
            displayName: messageResponse.data.displayName,
            avatarUrl: undefined, // Backend doesn't have avatarUrl yet
            createdAt: (messageResponse.data as any).createdAt,
            lastSeenAt: (messageResponse.data as any).lastSeenAt
          };
          console.log('🔐 Setting user:', user);
          setUser(user);
        } else {
          console.log('🔐 Invalid response, removing token');
          tokenStorage.remove();
          setToken(null);
        }
      })
      .catch((error) => {
        console.log('🔐 /api/auth/me failed:', error);
        // Check if token was removed due to security reasons
        if (tokenStorage.wasTokenRemoved()) {
          setSecurityLogout(true);
        }
        tokenStorage.remove();
        setToken(null);
      })
      .finally(() => {
        console.log('🔐 Setting isLoading to false');
        setIsLoading(false);
      });
    } else {
      console.log('🔐 No token, setting isLoading to false');
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
        
        // Store token persistently so it survives page reloads
        tokenStorage.set(newToken, true); // Persist for better UX
        setToken(newToken);
        setUser(user);
        
        toast.success(messageResponse.message || 'Login successful');
      } else {
        toast.error(messageResponse.message || 'Login failed');
        throw new Error(messageResponse.message || 'Login failed');
      }
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage = axiosError.response?.data?.message || axiosError.message || 'Login failed';
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

        // Store token persistently so it survives page reloads
        tokenStorage.set(newToken, true); // Persist for better UX
        setToken(newToken);
        setUser(user);

        toast.success(messageResponse.message || 'Registration successful');
      } else {
        toast.error(messageResponse.message || 'Registration failed');
        throw new Error(messageResponse.message || 'Registration failed');
      }
    } catch (error: unknown) {
      const axiosError = error as { response?: { data?: { message?: string } }; message?: string };
      const errorMessage = axiosError.response?.data?.message || axiosError.message || 'Registration failed';
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

  const updateUserProfile = async (updates: Partial<User>) => {
    if (!token) {
      throw new Error('No authentication token available');
    }

    try {
      const response = await axios.put(`${apiUrl}/api/auth/profile`, updates, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      const messageResponse = response.data as MessageResponse<User>;
      
      if (messageResponse.success && messageResponse.data) {
        // Update local user state with the updated data from server
        const updatedUser: User = {
          id: messageResponse.data.id,
          username: messageResponse.data.username,
          email: messageResponse.data.email,
          displayName: messageResponse.data.displayName,
          avatarUrl: messageResponse.data.avatarUrl,
          createdAt: (messageResponse.data as any).createdAt,
          lastSeenAt: (messageResponse.data as any).lastSeenAt
        };
        setUser(updatedUser);
      } else {
        throw new Error(messageResponse.message || 'Failed to update profile');
      }
    } catch (error: unknown) {
      console.error('Failed to update profile:', error);
      const axiosError = error as { response?: { status?: number } };
      if (axiosError.response?.status === 401) {
        // Token expired, trigger logout
        logout();
        throw new Error('Session expired. Please log in again.');
      }
      throw error;
    }
  };

  const dismissSecurityNotification = () => {
    setSecurityLogout(false);
    tokenStorage.clearSecurityFlag();
  };

  return (
    <AuthContext.Provider value={{ 
      user, 
      token, 
      login, 
      register, 
      logout, 
      updateUserProfile,
      isLoading, 
      securityLogout, 
      dismissSecurityNotification 
    }}>
      {children}
    </AuthContext.Provider>
  );
};
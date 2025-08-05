import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
import axios from 'axios';
import toast from 'react-hot-toast';
import { AuthProvider, useAuth } from '../useAuth';

// Mock dependencies
jest.mock('axios');
jest.mock('react-hot-toast');

const mockedAxios = axios as jest.Mocked<typeof axios>;
const mockedToast = toast as jest.Mocked<typeof toast>;

// Test component to use the hook
const TestComponent: React.FC = () => {
  const { user, token, login, register, logout, isLoading } = useAuth();
  
  return (
    <div>
      <div data-testid="user">{user ? user.username : 'null'}</div>
      <div data-testid="token">{token || 'null'}</div>
      <div data-testid="loading">{isLoading.toString()}</div>
      <button data-testid="login" onClick={() => login('test@example.com', 'password')}>
        Login
      </button>
      <button data-testid="register" onClick={() => register('testuser', 'test@example.com', 'password', 'Test User')}>
        Register
      </button>
      <button data-testid="logout" onClick={logout}>
        Logout
      </button>
    </div>
  );
};

// Wrapper component
const Wrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <AuthProvider>{children}</AuthProvider>
);

describe('useAuth Hook', () => {
  beforeEach(() => {
    // Clear localStorage
    localStorage.clear();
    
    // Reset mocks
    jest.clearAllMocks();
    
    // Mock console.warn to avoid noise
    jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('should initialize with no user and loading false when no token in localStorage', async () => {
    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    
    expect(screen.getByTestId('user')).toHaveTextContent('null');
    expect(screen.getByTestId('token')).toHaveTextContent('null');
  });

  it('should fetch user data when token exists in localStorage', async () => {
    const mockUser = {
      id: '1',
      username: 'testuser',
      email: 'test@example.com',
      displayName: 'Test User'
    };
    
    // Set token in localStorage
    localStorage.setItem('token', 'test-token');
    
    // Mock successful API call
    mockedAxios.get.mockResolvedValueOnce({
      data: mockUser
    });

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    
    expect(screen.getByTestId('user')).toHaveTextContent('testuser');
    expect(screen.getByTestId('token')).toHaveTextContent('test-token');

    expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/auth/me', {
      headers: { Authorization: 'Bearer test-token' }
    });
  });

  it('should clear token when API call fails', async () => {
    // Set token in localStorage
    localStorage.setItem('token', 'invalid-token');
    
    // Mock failed API call
    mockedAxios.get.mockRejectedValueOnce(new Error('Unauthorized'));

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    
    expect(screen.getByTestId('user')).toHaveTextContent('null');
    expect(screen.getByTestId('token')).toHaveTextContent('null');

    expect(localStorage.getItem('token')).toBeNull();
  });

  it('should login successfully', async () => {
    const mockResponse = {
      data: {
        token: 'new-token',
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User'
        }
      }
    };

    mockedAxios.post.mockResolvedValueOnce(mockResponse);

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    // Wait for initial loading to complete
    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    // Trigger login
    await act(async () => {
      screen.getByTestId('login').click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser');
    });
    
    expect(screen.getByTestId('token')).toHaveTextContent('new-token');

    expect(mockedAxios.post).toHaveBeenCalledWith('http://localhost:8080/api/auth/login', {
      email: 'test@example.com',
      password: 'password'
    });
    expect(mockedToast.success).toHaveBeenCalledWith('Login successful');
    expect(localStorage.getItem('token')).toBe('new-token');
  });

  it('should handle login failure', async () => {
    const mockError = {
      response: {
        data: {
          error: 'Invalid credentials'
        }
      }
    };

    mockedAxios.post.mockRejectedValueOnce(mockError);

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    // Wait for initial loading to complete
    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    // Trigger login
    await act(async () => {
      screen.getByTestId('login').click();
    });

    await waitFor(() => {
      expect(mockedToast.error).toHaveBeenCalledWith('Invalid credentials');
    });

    expect(screen.getByTestId('user')).toHaveTextContent('null');
    expect(screen.getByTestId('token')).toHaveTextContent('null');
  });

  it('should register successfully', async () => {
    const mockResponse = {
      data: {
        token: 'new-token',
        user: {
          id: '1',
          username: 'testuser',
          email: 'test@example.com',
          displayName: 'Test User'
        }
      }
    };

    mockedAxios.post.mockResolvedValueOnce(mockResponse);

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    // Wait for initial loading to complete
    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    // Trigger register
    await act(async () => {
      screen.getByTestId('register').click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser');
    });
    
    expect(screen.getByTestId('token')).toHaveTextContent('new-token');

    expect(mockedAxios.post).toHaveBeenCalledWith('http://localhost:8080/api/auth/register', {
      username: 'testuser',
      email: 'test@example.com',
      password: 'password',
      displayName: 'Test User'
    });
    expect(mockedToast.success).toHaveBeenCalledWith('Registration successful');
    expect(localStorage.getItem('token')).toBe('new-token');
  });

  it('should handle registration failure', async () => {
    const mockError = {
      response: {
        data: {
          error: 'Username already exists'
        }
      }
    };

    mockedAxios.post.mockRejectedValueOnce(mockError);

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    // Wait for initial loading to complete
    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });

    // Trigger register
    await act(async () => {
      screen.getByTestId('register').click();
    });

    await waitFor(() => {
      expect(mockedToast.error).toHaveBeenCalledWith('Username already exists');
    });

    expect(screen.getByTestId('user')).toHaveTextContent('null');
    expect(screen.getByTestId('token')).toHaveTextContent('null');
  });

  it('should logout successfully', async () => {
    const mockUser = {
      id: '1',
      username: 'testuser',
      email: 'test@example.com',
      displayName: 'Test User'
    };
    
    // Set token and mock user fetch
    localStorage.setItem('token', 'test-token');
    mockedAxios.get.mockResolvedValueOnce({ data: mockUser });
    mockedAxios.post.mockResolvedValueOnce({});

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    // Wait for user to load
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser');
    });

    // Trigger logout
    await act(async () => {
      screen.getByTestId('logout').click();
    });

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('null');
    });
    
    expect(screen.getByTestId('token')).toHaveTextContent('null');

    expect(mockedAxios.post).toHaveBeenCalledWith('http://localhost:8080/api/auth/logout', {}, {
      headers: { Authorization: 'Bearer test-token' }
    });
    expect(mockedToast.success).toHaveBeenCalledWith('Logged out successfully');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('should handle logout failure gracefully', async () => {
    const mockUser = {
      id: '1',
      username: 'testuser',
      email: 'test@example.com',
      displayName: 'Test User'
    };
    
    // Set token and mock user fetch
    localStorage.setItem('token', 'test-token');
    mockedAxios.get.mockResolvedValueOnce({ data: mockUser });
    mockedAxios.post.mockRejectedValueOnce(new Error('Network error'));

    render(
      <Wrapper>
        <TestComponent />
      </Wrapper>
    );

    // Wait for user to load
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('testuser');
    });

    // Trigger logout
    await act(async () => {
      screen.getByTestId('logout').click();
    });

    // Should still logout locally even if API call fails
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('null');
    });
    
    expect(screen.getByTestId('token')).toHaveTextContent('null');

    expect(mockedToast.success).toHaveBeenCalledWith('Logged out successfully');
    expect(localStorage.getItem('token')).toBeNull();
  });
});
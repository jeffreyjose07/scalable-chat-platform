import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import LoginPage from '../LoginPage';
import { useAuth } from '../../hooks/useAuth';

// Mock the useAuth hook
jest.mock('../../hooks/useAuth');

const mockedUseAuth = useAuth as jest.MockedFunction<typeof useAuth>;

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  Navigate: ({ to }: { to: string }) => <div data-testid="navigate">{to}</div>
}));

describe('LoginPage', () => {
  const mockLogin = jest.fn();
  const mockRegister = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    
    mockedUseAuth.mockReturnValue({
      user: null,
      token: null,
      login: mockLogin,
      register: mockRegister,
      logout: jest.fn(),
      isLoading: false
    });
  });

  const renderLoginPage = () => {
    return render(
      <BrowserRouter>
        <LoginPage />
      </BrowserRouter>
    );
  };

  it('should render login form by default', () => {
    renderLoginPage();

    expect(screen.getByText('Sign in to Chat Platform')).toBeInTheDocument();
    expect(screen.getByText('Welcome back! Please sign in to continue')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Email address')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password (minimum 6 characters)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByText("Don't have an account? Create one")).toBeInTheDocument();
  });

  it('should toggle to registration mode', () => {
    renderLoginPage();

    fireEvent.click(screen.getByText("Don't have an account? Create one"));

    expect(screen.getByText('Create Account')).toBeInTheDocument();
    expect(screen.getByText('Join the conversation today')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Display Name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Email address')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password (minimum 6 characters)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create Account' })).toBeInTheDocument();
    expect(screen.getByText('Already have an account? Sign in')).toBeInTheDocument();
  });

  it('should toggle back to login mode', () => {
    renderLoginPage();

    // Switch to register mode
    fireEvent.click(screen.getByText("Don't have an account? Create one"));
    expect(screen.getByText('Create Account')).toBeInTheDocument();

    // Switch back to login mode
    fireEvent.click(screen.getByText('Already have an account? Sign in'));
    expect(screen.getByText('Sign in to Chat Platform')).toBeInTheDocument();
  });

  it('should handle login form submission', async () => {
    renderLoginPage();

    // Fill in login form
    fireEvent.change(screen.getByPlaceholderText('Email address'), {
      target: { value: 'test@example.com' }
    });
    fireEvent.change(screen.getByPlaceholderText('Password (minimum 6 characters)'), {
      target: { value: 'password123' }
    });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
    });
  });

  it('should handle registration form submission', async () => {
    renderLoginPage();

    // Switch to registration mode
    fireEvent.click(screen.getByText("Don't have an account? Create one"));

    // Fill in registration form
    fireEvent.change(screen.getByPlaceholderText('Username'), {
      target: { value: 'testuser' }
    });
    fireEvent.change(screen.getByPlaceholderText('Display Name'), {
      target: { value: 'Test User' }
    });
    fireEvent.change(screen.getByPlaceholderText('Email address'), {
      target: { value: 'test@example.com' }
    });
    fireEvent.change(screen.getByPlaceholderText('Password (minimum 6 characters)'), {
      target: { value: 'password123' }
    });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: 'Create Account' }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith('testuser', 'test@example.com', 'password123', 'Test User');
    });
  });

  it('should show loading state during login', () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      token: null,
      login: mockLogin,
      register: mockRegister,
      logout: jest.fn(),
      isLoading: true
    });

    renderLoginPage();

    expect(screen.getByRole('button', { name: 'Signing in...' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Signing in...' })).toBeDisabled();
  });

  it('should show loading state during registration', () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      token: null,
      login: mockLogin,
      register: mockRegister,
      logout: jest.fn(),
      isLoading: true
    });

    renderLoginPage();

    // Switch to registration mode
    fireEvent.click(screen.getByText("Don't have an account? Create one"));

    expect(screen.getByRole('button', { name: 'Creating Account...' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Creating Account...' })).toBeDisabled();
  });

  it('should redirect to chat if user is already logged in', () => {
    mockedUseAuth.mockReturnValue({
      user: {
        id: '1',
        username: 'testuser',
        email: 'test@example.com',
        displayName: 'Test User'
      },
      token: 'test-token',
      login: mockLogin,
      register: mockRegister,
      logout: jest.fn(),
      isLoading: false
    });

    renderLoginPage();

    expect(screen.getByTestId('navigate')).toHaveTextContent('/chat');
  });

  it('should require all fields for registration', () => {
    renderLoginPage();

    // Switch to registration mode
    fireEvent.click(screen.getByText("Don't have an account? Create one"));

    // Check that all fields are required
    expect(screen.getByPlaceholderText('Username')).toBeRequired();
    expect(screen.getByPlaceholderText('Display Name')).toBeRequired();
    expect(screen.getByPlaceholderText('Email address')).toBeRequired();
    expect(screen.getByPlaceholderText('Password (minimum 6 characters)')).toBeRequired();
  });

  it('should require all fields for login', () => {
    renderLoginPage();

    // Check that all fields are required
    expect(screen.getByPlaceholderText('Email address')).toBeRequired();
    expect(screen.getByPlaceholderText('Password (minimum 6 characters)')).toBeRequired();
  });

  it('should have minimum password length validation', () => {
    renderLoginPage();

    const passwordInput = screen.getByPlaceholderText('Password (minimum 6 characters)');
    expect(passwordInput).toHaveAttribute('minLength', '6');
  });

  it('should handle login failure gracefully', async () => {
    mockLogin.mockRejectedValueOnce(new Error('Login failed'));

    renderLoginPage();

    // Fill in login form
    fireEvent.change(screen.getByPlaceholderText('Email address'), {
      target: { value: 'test@example.com' }
    });
    fireEvent.change(screen.getByPlaceholderText('Password (minimum 6 characters)'), {
      target: { value: 'password123' }
    });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('test@example.com', 'password123');
    });

    // Should not crash or show error in component (errors handled by hook)
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('should handle registration failure gracefully', async () => {
    mockRegister.mockRejectedValueOnce(new Error('Registration failed'));

    renderLoginPage();

    // Switch to registration mode
    fireEvent.click(screen.getByText("Don't have an account? Create one"));

    // Fill in registration form
    fireEvent.change(screen.getByPlaceholderText('Username'), {
      target: { value: 'testuser' }
    });
    fireEvent.change(screen.getByPlaceholderText('Display Name'), {
      target: { value: 'Test User' }
    });
    fireEvent.change(screen.getByPlaceholderText('Email address'), {
      target: { value: 'test@example.com' }
    });
    fireEvent.change(screen.getByPlaceholderText('Password (minimum 6 characters)'), {
      target: { value: 'password123' }
    });

    // Submit form
    fireEvent.click(screen.getByRole('button', { name: 'Create Account' }));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith('testuser', 'test@example.com', 'password123', 'Test User');
    });

    // Should not crash or show error in component (errors handled by hook)
    expect(screen.getByRole('button', { name: 'Create Account' })).toBeInTheDocument();
  });
});
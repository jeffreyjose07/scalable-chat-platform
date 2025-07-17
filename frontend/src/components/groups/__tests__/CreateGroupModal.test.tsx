import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { CreateGroupModal } from '../CreateGroupModal';
import { api } from '../../../services/api';
import { User } from '../../../types/chat';

// Mock the API
jest.mock('../../../services/api', () => ({
  api: {
    conversation: {
      createGroup: jest.fn(),
    },
  },
}));

// Mock the useUsers hook
jest.mock('../../../hooks/useUsers', () => ({
  useUsers: () => ({
    users: [
      {
        id: 'user1',
        username: 'user1',
        email: 'user1@example.com',
        displayName: 'User One',
        displayNameOrUsername: 'User One',
        online: true,
      },
      {
        id: 'user2',
        username: 'user2',
        email: 'user2@example.com',
        displayName: 'User Two',
        displayNameOrUsername: 'User Two',
        online: false,
      },
    ],
    loading: false,
    error: null,
  }),
}));

const mockUsers: User[] = [
  {
    id: 'user1',
    username: 'user1',
    email: 'user1@example.com',
    displayName: 'User One',
    displayNameOrUsername: 'User One',
    online: true,
  },
  {
    id: 'user2',
    username: 'user2',
    email: 'user2@example.com',
    displayName: 'User Two',
    displayNameOrUsername: 'User Two',
    online: false,
  },
];

describe('CreateGroupModal', () => {
  const defaultProps = {
    isOpen: true,
    onClose: jest.fn(),
    onGroupCreated: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (api.conversation.createGroup as jest.Mock).mockResolvedValue({
      id: 'group1',
      name: 'Test Group',
      participants: mockUsers,
    });
  });

  it('renders modal when isOpen is true', () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    expect(screen.getByText('Create New Group')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Group name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Group description (optional)')).toBeInTheDocument();
  });

  it('does not render modal when isOpen is false', () => {
    render(<CreateGroupModal {...defaultProps} isOpen={false} />);
    
    expect(screen.queryByText('Create New Group')).not.toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const closeButton = screen.getByTitle('Close');
    fireEvent.click(closeButton);
    
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when cancel button is clicked', () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const cancelButton = screen.getByText('Cancel');
    fireEvent.click(cancelButton);
    
    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
  });

  it('allows entering group name and description', () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    const descriptionInput = screen.getByPlaceholderText('Group description (optional)');
    
    fireEvent.change(nameInput, { target: { value: 'Test Group' } });
    fireEvent.change(descriptionInput, { target: { value: 'Test Description' } });
    
    expect(nameInput).toHaveValue('Test Group');
    expect(descriptionInput).toHaveValue('Test Description');
  });

  it('allows selecting users for the group', async () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const userCheckbox = screen.getByLabelText('User One');
    fireEvent.click(userCheckbox);
    
    expect(userCheckbox).toBeChecked();
  });

  it('allows deselecting users', async () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const userCheckbox = screen.getByLabelText('User One');
    
    // Select user
    fireEvent.click(userCheckbox);
    expect(userCheckbox).toBeChecked();
    
    // Deselect user
    fireEvent.click(userCheckbox);
    expect(userCheckbox).not.toBeChecked();
  });

  it('allows toggling group privacy', () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const publicCheckbox = screen.getByLabelText('Public Group');
    
    expect(publicCheckbox).not.toBeChecked();
    
    fireEvent.click(publicCheckbox);
    expect(publicCheckbox).toBeChecked();
  });

  it('allows changing max participants', () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const maxParticipantsInput = screen.getByDisplayValue('100');
    fireEvent.change(maxParticipantsInput, { target: { value: '50' } });
    
    expect(maxParticipantsInput).toHaveValue(50);
  });

  it('shows validation error for empty group name', async () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const createButton = screen.getByText('Create Group');
    fireEvent.click(createButton);
    
    await waitFor(() => {
      expect(screen.getByText('Group name is required')).toBeInTheDocument();
    });
  });

  it('shows validation error for group name too long', async () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    const longName = 'a'.repeat(101);
    fireEvent.change(nameInput, { target: { value: longName } });
    
    const createButton = screen.getByText('Create Group');
    fireEvent.click(createButton);
    
    await waitFor(() => {
      expect(screen.getByText('Group name must be 100 characters or less')).toBeInTheDocument();
    });
  });

  it('shows validation error for description too long', async () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    const descriptionInput = screen.getByPlaceholderText('Group description (optional)');
    
    fireEvent.change(nameInput, { target: { value: 'Test Group' } });
    fireEvent.change(descriptionInput, { target: { value: 'a'.repeat(501) } });
    
    const createButton = screen.getByText('Create Group');
    fireEvent.click(createButton);
    
    await waitFor(() => {
      expect(screen.getByText('Description must be 500 characters or less')).toBeInTheDocument();
    });
  });

  it('shows validation error for invalid max participants', async () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    const maxParticipantsInput = screen.getByDisplayValue('100');
    
    fireEvent.change(nameInput, { target: { value: 'Test Group' } });
    fireEvent.change(maxParticipantsInput, { target: { value: '1' } });
    
    const createButton = screen.getByText('Create Group');
    fireEvent.click(createButton);
    
    await waitFor(() => {
      expect(screen.getByText('Max participants must be between 2 and 1000')).toBeInTheDocument();
    });
  });

  it('creates group successfully with valid data', async () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    const descriptionInput = screen.getByPlaceholderText('Group description (optional)');
    const userCheckbox = screen.getByLabelText('User One');
    const createButton = screen.getByText('Create Group');
    
    fireEvent.change(nameInput, { target: { value: 'Test Group' } });
    fireEvent.change(descriptionInput, { target: { value: 'Test Description' } });
    fireEvent.click(userCheckbox);
    fireEvent.click(createButton);
    
    await waitFor(() => {
      expect(api.conversation.createGroup).toHaveBeenCalledWith({
        name: 'Test Group',
        description: 'Test Description',
        isPublic: false,
        maxParticipants: 100,
        participantIds: ['user1'],
      });
    });
    
    expect(defaultProps.onGroupCreated).toHaveBeenCalledWith({
      id: 'group1',
      name: 'Test Group',
      participants: mockUsers,
    });
  });

  it('handles API error gracefully', async () => {
    (api.conversation.createGroup as jest.Mock).mockRejectedValue(new Error('API Error'));
    
    render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    const userCheckbox = screen.getByLabelText('User One');
    const createButton = screen.getByText('Create Group');
    
    fireEvent.change(nameInput, { target: { value: 'Test Group' } });
    fireEvent.click(userCheckbox);
    fireEvent.click(createButton);
    
    await waitFor(() => {
      expect(screen.getByText('API Error')).toBeInTheDocument();
    });
  });

  it('shows loading state while creating group', async () => {
    (api.conversation.createGroup as jest.Mock).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    );
    
    render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    const userCheckbox = screen.getByLabelText('User One');
    const createButton = screen.getByText('Create Group');
    
    fireEvent.change(nameInput, { target: { value: 'Test Group' } });
    fireEvent.click(userCheckbox);
    fireEvent.click(createButton);
    
    expect(screen.getByText('Creating...')).toBeInTheDocument();
    expect(createButton).toBeDisabled();
  });

  it('resets form when modal is closed and reopened', () => {
    const { rerender } = render(<CreateGroupModal {...defaultProps} />);
    
    const nameInput = screen.getByPlaceholderText('Group name');
    fireEvent.change(nameInput, { target: { value: 'Test Group' } });
    
    // Close modal
    rerender(<CreateGroupModal {...defaultProps} isOpen={false} />);
    
    // Reopen modal
    rerender(<CreateGroupModal {...defaultProps} isOpen={true} />);
    
    const newNameInput = screen.getByPlaceholderText('Group name');
    expect(newNameInput).toHaveValue('');
  });

  it('displays user list correctly', () => {
    render(<CreateGroupModal {...defaultProps} />);
    
    expect(screen.getByText('User One')).toBeInTheDocument();
    expect(screen.getByText('User Two')).toBeInTheDocument();
    expect(screen.getByText('user1@example.com')).toBeInTheDocument();
    expect(screen.getByText('user2@example.com')).toBeInTheDocument();
  });

  it('handles empty user list', () => {
    // Mock empty users
    jest.doMock('../../../hooks/useUsers', () => ({
      useUsers: () => ({
        users: [],
        loading: false,
        error: null,
      }),
    }));
    
    render(<CreateGroupModal {...defaultProps} />);
    
    expect(screen.getByText('No users available')).toBeInTheDocument();
  });
});
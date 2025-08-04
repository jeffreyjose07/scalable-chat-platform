import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ConversationList from '../ConversationList';
import { Conversation, ConversationType, User, ConversationParticipant, ParticipantRole } from '../../types/chat';

// Mock ConversationTypeToggle component
jest.mock('../ConversationTypeToggle', () => ({
  __esModule: true,
  default: ({ activeType, onTypeChange }: any) => (
    <div data-testid="conversation-type-toggle">
      <button
        data-testid="direct-button"
        onClick={() => onTypeChange('direct')}
        className={activeType === 'direct' ? 'active' : ''}
      >
        Direct
      </button>
      <button
        data-testid="groups-button"
        onClick={() => onTypeChange('groups')}
        className={activeType === 'groups' ? 'active' : ''}
      >
        Groups
      </button>
    </div>
  ),
}));

const mockCurrentUser: User = {
  id: 'user1',
  username: 'testuser',
  email: 'test@example.com',
  displayName: 'Test User',
  displayNameOrUsername: 'Test User',
  online: true,
};

const mockOtherUser: User = {
  id: 'user2',
  username: 'otheruser',
  email: 'other@example.com',
  displayName: 'Other User',
  displayNameOrUsername: 'Other User',
  online: false,
};

const mockDirectConversation: Conversation = {
  id: 'conv1',
  name: '',
  type: ConversationType.DIRECT,
  participants: [
    { user: mockCurrentUser, role: ParticipantRole.MEMBER },
    { user: mockOtherUser, role: ParticipantRole.MEMBER }
  ],
  updatedAt: '2023-01-01T00:00:00Z',
  createdAt: '2023-01-01T00:00:00Z',
};

const mockGroupConversation: Conversation = {
  id: 'conv2',
  name: 'Test Group',
  type: ConversationType.GROUP,
  participants: [
    { user: mockCurrentUser, role: ParticipantRole.OWNER },
    { user: mockOtherUser, role: ParticipantRole.MEMBER }
  ],
  updatedAt: '2023-01-01T00:00:00Z',
  createdAt: '2023-01-01T00:00:00Z',
};

describe('ConversationList', () => {
  const defaultProps = {
    selectedConversation: '',
    onSelectConversation: jest.fn(),
    conversations: [mockDirectConversation, mockGroupConversation],
    activeType: 'direct' as const,
    onTypeChange: jest.fn(),
    onNewDirectMessage: jest.fn(),
    onNewGroup: jest.fn(),
    unreadCounts: {},
    currentUserId: 'user1',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders conversation list with correct structure', () => {
    render(<ConversationList {...defaultProps} />);
    
    expect(screen.getByTestId('conversation-type-toggle')).toBeInTheDocument();
    expect(screen.getByText('Direct Messages')).toBeInTheDocument();
  });

  it('displays direct conversations when activeType is direct', () => {
    render(<ConversationList {...defaultProps} activeType="direct" />);
    
    expect(screen.getByText('Direct Messages')).toBeInTheDocument();
    expect(screen.getByText('Other User')).toBeInTheDocument();
    expect(screen.queryByText('Test Group')).not.toBeInTheDocument();
  });

  it('displays group conversations when activeType is groups', () => {
    render(<ConversationList {...defaultProps} activeType="groups" />);
    
    expect(screen.getByText('Group Conversations')).toBeInTheDocument();
    expect(screen.getByText('Test Group')).toBeInTheDocument();
    expect(screen.queryByText('Other User')).not.toBeInTheDocument();
  });

  it('shows new direct message button when activeType is direct', () => {
    render(<ConversationList {...defaultProps} activeType="direct" />);
    
    const newDirectButton = screen.getByTitle('New Direct Message');
    expect(newDirectButton).toBeInTheDocument();
    
    fireEvent.click(newDirectButton);
    expect(defaultProps.onNewDirectMessage).toHaveBeenCalledTimes(1);
  });

  it('shows new group button when activeType is groups', () => {
    render(<ConversationList {...defaultProps} activeType="groups" />);
    
    const newGroupButton = screen.getByTitle('Create New Group');
    expect(newGroupButton).toBeInTheDocument();
    
    fireEvent.click(newGroupButton);
    expect(defaultProps.onNewGroup).toHaveBeenCalledTimes(1);
  });

  it('calls onSelectConversation when conversation is clicked', () => {
    render(<ConversationList {...defaultProps} />);
    
    const conversationButton = screen.getByText('Other User').closest('button');
    fireEvent.click(conversationButton!);
    
    expect(defaultProps.onSelectConversation).toHaveBeenCalledWith('conv1');
  });

  it('displays unread counts correctly', () => {
    const propsWithUnread = {
      ...defaultProps,
      unreadCounts: { conv1: 3 },
    };
    
    render(<ConversationList {...propsWithUnread} />);
    
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('shows empty state for direct messages when no conversations', () => {
    render(<ConversationList {...defaultProps} conversations={[]} activeType="direct" />);
    
    expect(screen.getByText('No direct messages yet')).toBeInTheDocument();
    expect(screen.getByText('Start a conversation')).toBeInTheDocument();
  });

  it('shows empty state for groups when no conversations', () => {
    render(<ConversationList {...defaultProps} conversations={[]} activeType="groups" />);
    
    expect(screen.getByText('No group conversations yet')).toBeInTheDocument();
    expect(screen.getByText('Create a group')).toBeInTheDocument();
  });

  it('handles conversation selection highlighting', () => {
    render(<ConversationList {...defaultProps} selectedConversation="conv1" />);
    
    const selectedConversation = screen.getByText('Other User').closest('button');
    expect(selectedConversation).toHaveClass('bg-blue-100', 'text-blue-700');
  });

  it('displays correct conversation display name for direct messages', () => {
    render(<ConversationList {...defaultProps} />);
    
    // Should show the other participant's display name
    expect(screen.getByText('Other User')).toBeInTheDocument();
  });

  it('displays correct conversation display name for groups', () => {
    render(<ConversationList {...defaultProps} activeType="groups" />);
    
    // Should show the group name
    expect(screen.getByText('Test Group')).toBeInTheDocument();
  });

  it('handles missing onNewGroup prop gracefully', () => {
    const propsWithoutOnNewGroup = {
      ...defaultProps,
      onNewGroup: undefined,
    };
    
    render(<ConversationList {...propsWithoutOnNewGroup} activeType="groups" />);
    
    // Should not show the new group button
    expect(screen.queryByTitle('Create New Group')).not.toBeInTheDocument();
  });

  it('filters conversations correctly based on type', () => {
    const conversations = [
      mockDirectConversation,
      mockGroupConversation,
      {
        ...mockDirectConversation,
        id: 'conv3',
        participants: [{ user: mockCurrentUser, role: ParticipantRole.MEMBER }],
      },
    ];
    
    render(<ConversationList {...defaultProps} conversations={conversations} activeType="direct" />);
    
    // Should show only direct conversations
    expect(screen.getAllByText(/Other User|Test User/)).toHaveLength(2); // 2 direct conversations
    expect(screen.queryByText('Test Group')).not.toBeInTheDocument();
  });
});
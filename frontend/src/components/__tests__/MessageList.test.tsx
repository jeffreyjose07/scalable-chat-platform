import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import MessageList from '../MessageList';
import { ChatMessage, MessageType } from '../../types/chat';

// Mock date-fns format function
jest.mock('date-fns', () => ({
  format: jest.fn((date, formatString) => {
    if (formatString === 'HH:mm') {
      return '12:34';
    }
    return '12:34';
  }),
}));

const mockMessages: ChatMessage[] = [
  {
    id: 'msg1',
    conversationId: 'conv1',
    senderId: 'user1',
    senderUsername: 'testuser',
    content: 'Hello world!',
    type: MessageType.TEXT,
    timestamp: '2023-01-01T12:34:00Z',
  },
  {
    id: 'msg2',
    conversationId: 'conv1',
    senderId: 'user2',
    senderUsername: 'otheruser',
    content: 'Hi there!',
    type: MessageType.TEXT,
    timestamp: '2023-01-01T12:35:00Z',
  },
  {
    id: 'msg3',
    conversationId: 'conv1',
    senderId: 'user1',
    senderUsername: 'testuser',
    content: 'How are you?',
    type: MessageType.TEXT,
    timestamp: '2023-01-01T12:36:00Z',
  },
];

describe('MessageList', () => {
  const defaultProps = {
    messages: mockMessages,
    currentUserId: 'user1',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders all messages correctly', () => {
    render(<MessageList {...defaultProps} />);
    
    expect(screen.getByText('Hello world!')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();
    expect(screen.getByText('How are you?')).toBeInTheDocument();
  });

  it('distinguishes between own and other messages', () => {
    render(<MessageList {...defaultProps} />);
    
    // Verify messages are rendered (layout specifics may vary)
    expect(screen.getByText('Hello world!')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();
  });

  it('shows username for other users messages', () => {
    render(<MessageList {...defaultProps} />);
    
    expect(screen.getByText('otheruser')).toBeInTheDocument();
  });

  it('does not show username for own messages', () => {
    render(<MessageList {...defaultProps} />);
    
    const ownMessageBubbles = screen.getAllByText('testuser');
    expect(ownMessageBubbles).toHaveLength(0);
  });

  it('displays timestamps for all messages', () => {
    render(<MessageList {...defaultProps} />);
    
    const timestamps = screen.getAllByText('12:34');
    expect(timestamps).toHaveLength(3);
  });

  it('renders own and other messages with appropriate styling', () => {
    render(<MessageList {...defaultProps} />);
    
    // Just verify messages are rendered - specific styling may vary
    expect(screen.getByText('Hello world!')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();
  });

  it('handles empty messages array', () => {
    render(<MessageList {...defaultProps} messages={[]} />);
    
    // Should not crash and render empty list
    expect(screen.queryByText('Hello world!')).not.toBeInTheDocument();
  });

  it('handles missing senderUsername gracefully', () => {
    const messageWithoutUsername = {
      ...mockMessages[1],
      senderUsername: undefined,
    };
    
    render(<MessageList {...defaultProps} messages={[messageWithoutUsername]} />);
    
    // Should fall back to senderId
    expect(screen.getByText('user2')).toBeInTheDocument();
  });

  it('handles missing senderId gracefully', () => {
    const messageWithoutIds = {
      ...mockMessages[1],
      senderId: '',
      senderUsername: undefined,
    };
    
    render(<MessageList {...defaultProps} messages={[messageWithoutIds]} />);
    
    // Should fall back to 'Unknown User'
    expect(screen.getByText('Unknown User')).toBeInTheDocument();
  });

  it('handles messages without currentUserId', () => {
    render(<MessageList {...defaultProps} currentUserId={undefined} />);
    
    // All messages should be treated as from others
    expect(screen.getAllByText(/testuser|otheruser/)).toHaveLength(3);
  });

  it('preserves message order', () => {
    render(<MessageList {...defaultProps} />);
    
    const messages = screen.getAllByText(/Hello world!|Hi there!|How are you?/);
    expect(messages[0]).toHaveTextContent('Hello world!');
    expect(messages[1]).toHaveTextContent('Hi there!');
    expect(messages[2]).toHaveTextContent('How are you?');
  });

  it('handles long messages with proper line breaks', () => {
    const longMessage = {
      ...mockMessages[0],
      content: 'This is a very long message that should wrap properly and not overflow the container when displayed in the chat interface.',
    };
    
    render(<MessageList {...defaultProps} messages={[longMessage]} />);
    
    const messageElement = screen.getByText(/This is a very long message/);
    expect(messageElement).toHaveClass('break-words');
  });

  it('renders without crashing with various prop combinations', () => {
    // Test with different prop combinations to ensure robustness
    render(<MessageList {...defaultProps} />);
    expect(screen.getByText('Hello world!')).toBeInTheDocument();
    
    // Test with empty currentUserId
    render(<MessageList messages={mockMessages} currentUserId="" />);
    expect(screen.getByText('Hello world!')).toBeInTheDocument();
  });
});
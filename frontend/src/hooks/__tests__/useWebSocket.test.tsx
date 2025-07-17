import React from 'react';
import { renderHook, act } from '@testing-library/react';
import { useWebSocket, WebSocketProvider } from '../useWebSocket';
import { useAuth } from '../useAuth';
import { messageService } from '../../services/messageService';
import { getWebSocketUrl } from '../../utils/networkUtils';

// Mock dependencies
jest.mock('../useAuth');
jest.mock('../../services/messageService');
jest.mock('../../utils/networkUtils');
jest.mock('react-hot-toast', () => ({
  __esModule: true,
  default: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

// Mock WebSocket
class MockWebSocket {
  public onopen: ((event: Event) => void) | null = null;
  public onclose: ((event: CloseEvent) => void) | null = null;
  public onmessage: ((event: MessageEvent) => void) | null = null;
  public onerror: ((event: Event) => void) | null = null;
  public readyState: number = WebSocket.CONNECTING;
  public url: string;

  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  constructor(url: string) {
    this.url = url;
    setTimeout(() => {
      this.readyState = WebSocket.OPEN;
      this.onopen?.(new Event('open'));
    }, 0);
  }

  send(data: string) {
    // Mock send implementation
  }

  close() {
    this.readyState = WebSocket.CLOSED;
    this.onclose?.(new CloseEvent('close', { code: 1000, reason: 'Normal closure' }));
  }
}

(global as any).WebSocket = MockWebSocket;

const mockUser = {
  id: 'user1',
  username: 'testuser',
  email: 'test@example.com',
  displayName: 'Test User',
  displayNameOrUsername: 'Test User',
  online: true,
};

const mockToken = 'mock-jwt-token';

const mockMessages = [
  {
    id: 'msg1',
    conversationId: 'conv1',
    senderId: 'user1',
    senderUsername: 'testuser',
    content: 'Hello',
    type: 'TEXT',
    timestamp: '2023-01-01T00:00:00Z',
  },
];

describe('useWebSocket', () => {
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <WebSocketProvider>{children}</WebSocketProvider>
  );

  beforeEach(() => {
    jest.clearAllMocks();
    (useAuth as jest.Mock).mockReturnValue({
      user: mockUser,
      token: mockToken,
    });
    (getWebSocketUrl as jest.Mock).mockReturnValue('ws://localhost:8080');
    (messageService.fetchRecentMessages as jest.Mock).mockResolvedValue(mockMessages);
  });

  it('provides initial WebSocket state', () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    expect(result.current.socket).toBeNull();
    expect(result.current.isConnected).toBe(false);
    expect(result.current.messages).toEqual([]);
  });

  it('connects to WebSocket when user and token are available', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    expect(result.current.socket).not.toBeNull();
    expect(result.current.isConnected).toBe(true);
  });

  it('does not connect when user is not available', () => {
    (useAuth as jest.Mock).mockReturnValue({
      user: null,
      token: mockToken,
    });

    const { result } = renderHook(() => useWebSocket(), { wrapper });

    expect(result.current.socket).toBeNull();
    expect(result.current.isConnected).toBe(false);
  });

  it('does not connect when token is not available', () => {
    (useAuth as jest.Mock).mockReturnValue({
      user: mockUser,
      token: null,
    });

    const { result } = renderHook(() => useWebSocket(), { wrapper });

    expect(result.current.socket).toBeNull();
    expect(result.current.isConnected).toBe(false);
  });

  it('loads initial messages on connection', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    expect(messageService.fetchRecentMessages).toHaveBeenCalledWith(mockToken);
    expect(result.current.messages).toEqual(mockMessages);
  });

  it('handles incoming messages', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const newMessage = {
      id: 'msg2',
      conversationId: 'conv1',
      senderId: 'user2',
      senderUsername: 'otheruser',
      content: 'Hi there',
      type: 'TEXT',
      timestamp: '2023-01-01T00:01:00Z',
    };

    await act(async () => {
      const socket = result.current.socket as any;
      socket.onmessage({
        data: JSON.stringify(newMessage),
      });
    });

    expect(result.current.messages).toHaveLength(2);
    expect(result.current.messages[1]).toEqual(newMessage);
  });

  it('handles duplicate messages', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const duplicateMessage = mockMessages[0];

    await act(async () => {
      const socket = result.current.socket as any;
      socket.onmessage({
        data: JSON.stringify(duplicateMessage),
      });
    });

    expect(result.current.messages).toHaveLength(1);
  });

  it('handles acknowledgment messages', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const ackMessage = {
      type: 'ack',
      messageId: 'msg1',
    };

    await act(async () => {
      const socket = result.current.socket as any;
      socket.onmessage({
        data: JSON.stringify(ackMessage),
      });
    });

    // Should not add ack message to messages array
    expect(result.current.messages).toHaveLength(1);
  });

  it('handles error messages', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const errorMessage = {
      type: 'error',
      message: 'Something went wrong',
    };

    await act(async () => {
      const socket = result.current.socket as any;
      socket.onmessage({
        data: JSON.stringify(errorMessage),
      });
    });

    // Should not add error message to messages array
    expect(result.current.messages).toHaveLength(1);
  });

  it('sends messages through WebSocket', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const mockSend = jest.fn();
    (result.current.socket as any).send = mockSend;

    const messageToSend = {
      conversationId: 'conv1',
      senderId: 'user1',
      senderUsername: 'testuser',
      content: 'Hello World',
      type: 'TEXT' as const,
    };

    await act(async () => {
      result.current.sendMessage(messageToSend);
    });

    expect(mockSend).toHaveBeenCalledWith(
      expect.stringContaining('"content":"Hello World"')
    );
  });

  it('prevents sending empty messages', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const mockSend = jest.fn();
    (result.current.socket as any).send = mockSend;

    const emptyMessage = {
      conversationId: 'conv1',
      senderId: 'user1',
      senderUsername: 'testuser',
      content: '   ',
      type: 'TEXT' as const,
    };

    await act(async () => {
      result.current.sendMessage(emptyMessage);
    });

    expect(mockSend).not.toHaveBeenCalled();
  });

  it('prevents sending when not connected', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    const messageToSend = {
      conversationId: 'conv1',
      senderId: 'user1',
      senderUsername: 'testuser',
      content: 'Hello World',
      type: 'TEXT' as const,
    };

    await act(async () => {
      result.current.sendMessage(messageToSend);
    });

    // Should not throw error, just silently fail
    expect(result.current.messages).toEqual([]);
  });

  it('handles WebSocket connection errors', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    await act(async () => {
      const socket = result.current.socket as any;
      socket.onerror(new Event('error'));
    });

    // Should handle error gracefully
    expect(result.current.isConnected).toBe(true); // Still connected until close event
  });

  it('handles WebSocket close events', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    await act(async () => {
      const socket = result.current.socket as any;
      socket.close();
    });

    expect(result.current.isConnected).toBe(false);
  });

  it('handles malformed JSON messages', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    await act(async () => {
      const socket = result.current.socket as any;
      socket.onmessage({
        data: 'invalid json',
      });
    });

    // Should not crash and messages should remain unchanged
    expect(result.current.messages).toEqual(mockMessages);
  });

  it('cleans up WebSocket on unmount', async () => {
    const { result, unmount } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const mockClose = jest.fn();
    (result.current.socket as any).close = mockClose;

    unmount();

    expect(mockClose).toHaveBeenCalled();
  });

  it('prevents duplicate connections', async () => {
    const { result, rerender } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const firstSocket = result.current.socket;

    // Force re-render
    rerender();

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    // Should be the same socket instance
    expect(result.current.socket).toBe(firstSocket);
  });

  it('prevents duplicate message sending', async () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 10));
    });

    const mockSend = jest.fn();
    (result.current.socket as any).send = mockSend;

    const messageToSend = {
      conversationId: 'conv1',
      senderId: 'user1',
      senderUsername: 'testuser',
      content: 'Hello World',
      type: 'TEXT' as const,
    };

    await act(async () => {
      result.current.sendMessage(messageToSend);
      result.current.sendMessage(messageToSend);
    });

    // Should only send once
    expect(mockSend).toHaveBeenCalledTimes(1);
  });
});
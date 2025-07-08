import { ChatMessage } from '../types/chat';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const messageService = {
  async fetchConversationMessages(conversationId: string, token: string): Promise<ChatMessage[]> {
    try {
      console.log(`Fetching messages for conversation: ${conversationId}`);
      const response = await fetch(`${API_BASE}/api/messages/conversations/${conversationId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
        console.error(`Failed to fetch messages: ${response.status}`, errorData);
        throw new Error(errorData.error || `HTTP ${response.status}`);
      }
      
      const messages = await response.json();
      console.log(`Fetched ${messages.length} messages for conversation: ${conversationId}`);
      return messages;
    } catch (error) {
      console.error('Error fetching conversation messages:', error);
      return [];
    }
  },

  async fetchRecentMessages(token: string): Promise<ChatMessage[]> {
    try {
      console.log('Fetching recent messages...');
      const response = await fetch(`${API_BASE}/api/messages/recent`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ error: 'Unknown error' }));
        console.error(`Failed to fetch recent messages: ${response.status}`, errorData);
        throw new Error(errorData.error || `HTTP ${response.status}`);
      }
      
      const messages = await response.json();
      console.log(`Fetched ${messages.length} recent messages`);
      return messages;
    } catch (error) {
      console.error('Error fetching recent messages:', error);
      return [];
    }
  },

  async fetchMessagesSince(conversationId: string, timestamp: string, token: string): Promise<ChatMessage[]> {
    try {
      const response = await fetch(`${API_BASE}/api/messages/conversations/${conversationId}/since/${timestamp}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (!response.ok) {
        throw new Error(`Failed to fetch messages since ${timestamp}: ${response.status}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Error fetching messages since timestamp:', error);
      return [];
    }
  }
};
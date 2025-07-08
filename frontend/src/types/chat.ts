export interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderUsername?: string; // Optional with fallback handling
  content: string;
  type: MessageType;
  timestamp: string;
}

export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  FILE = 'FILE',
  SYSTEM = 'SYSTEM'
}

export interface Conversation {
  id: string;
  name: string;
  participants: string[];
  lastMessage?: ChatMessage;
  updatedAt: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  isOnline: boolean;
}
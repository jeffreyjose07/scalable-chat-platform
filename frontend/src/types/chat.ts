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
  participants: ConversationParticipant[]; // Backend returns ConversationParticipantDto objects
  lastMessage?: ChatMessage;
  updatedAt: string;
  type?: ConversationType;
  description?: string;
  isPublic?: boolean;
  maxParticipants?: number;
  createdBy?: string;
  createdAt?: string;
}

export enum ConversationType {
  DIRECT = 'DIRECT',
  GROUP = 'GROUP'
}

export enum ParticipantRole {
  OWNER = 'OWNER',
  ADMIN = 'ADMIN',
  MEMBER = 'MEMBER'
}

export interface ConversationParticipant {
  user: User;
  role: ParticipantRole;
  joinedAt?: string;
  lastReadAt?: string;
}

export interface ConversationDto {
  id: string;
  name: string | null;
  description: string | null;
  type: ConversationType;
  isPublic: boolean;
  maxParticipants: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  participants: ConversationParticipant[];
  lastMessage?: ChatMessage;
  isGroup: boolean;
  isDirectMessage: boolean;
  unreadCount?: number;
}

export interface CreateGroupRequest {
  name: string;
  description: string | null;
  isPublic: boolean;
  maxParticipants: number;
  participantIds: string[];
}

export interface UpdateGroupSettingsRequest {
  name: string;
  description: string | null;
  isPublic: boolean;
  maxParticipants: number;
}

export interface User {
  id: string;
  username: string;
  email: string;
  displayName: string;
  avatarUrl?: string;
  lastSeenAt?: string;
  online?: boolean;
  displayNameOrUsername?: string;
  initials?: string;
  isOnline?: boolean; // Keep for backward compatibility
}
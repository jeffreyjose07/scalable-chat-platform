export interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderUsername?: string; // Optional with fallback handling
  content: string;
  type: MessageType;
  timestamp: string;
  status?: MessageStatus; // Optional for backward compatibility
  deliveredTo?: Record<string, string>; // userId -> timestamp (ISO string)
  readBy?: Record<string, string>; // userId -> timestamp (ISO string)
}

export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  FILE = 'FILE',
  SYSTEM = 'SYSTEM'
}

export enum MessageStatus {
  PENDING = 'PENDING',    // Message being sent/processing
  SENT = 'SENT',         // Message sent to server successfully  
  DELIVERED = 'DELIVERED', // Message delivered to recipient(s)
  READ = 'READ'          // Message read by recipient(s)
}

export interface MessageStatusUpdate {
  messageId: string;
  userId: string;
  statusType: 'DELIVERED' | 'READ';
  timestamp: string;
}

export interface WebSocketMessage {
  type: 'MESSAGE' | 'MESSAGE_DELIVERED' | 'MESSAGE_READ' | 'CONVERSATION_READ';
  data: ChatMessage | MessageStatusUpdate;
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
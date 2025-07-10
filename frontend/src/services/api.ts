import { User } from '../components/UserSearchModal';
import { Conversation } from '../components/ConversationList';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

// Helper function to get auth token
const getAuthToken = (): string | null => {
  return localStorage.getItem('token');
};

// Helper function to make authenticated requests
const authenticatedFetch = async (url: string, options: RequestInit = {}): Promise<Response> => {
  const token = getAuthToken();
  const startTime = Date.now();
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  console.log(`🌐 API Request: ${options.method || 'GET'} ${url}`, {
    headers: { ...headers, Authorization: token ? 'Bearer [REDACTED]' : undefined },
    body: options.body ? JSON.parse(options.body as string) : undefined
  });
  
  try {
    const response = await fetch(url, {
      ...options,
      headers,
    });
    
    const duration = Date.now() - startTime;
    
    if (!response.ok) {
      console.error(`❌ API Error: ${options.method || 'GET'} ${url}`, {
        status: response.status,
        statusText: response.statusText,
        duration: `${duration}ms`
      });
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    console.log(`✅ API Success: ${options.method || 'GET'} ${url}`, {
      status: response.status,
      duration: `${duration}ms`
    });
    
    return response;
  } catch (error) {
    const duration = Date.now() - startTime;
    console.error(`💥 API Exception: ${options.method || 'GET'} ${url}`, {
      error: error instanceof Error ? error.message : String(error),
      duration: `${duration}ms`
    });
    throw error;
  }
};

// User API functions
export const userApi = {
  searchUsers: async (query: string, page = 0, size = 20): Promise<User[]> => {
    console.log('🔍 Searching users:', { query, page, size });
    
    const params = new URLSearchParams({
      query,
      page: page.toString(),
      size: size.toString(),
    });
    
    const response = await authenticatedFetch(`${API_BASE_URL}/users/search?${params}`);
    const result = await response.json();
    console.log('✅ User search results:', result.length, 'users found');
    return result;
  },

  getUserSuggestions: async (limit = 10): Promise<User[]> => {
    console.log('💡 Getting user suggestions, limit:', limit);
    
    const params = new URLSearchParams({
      limit: limit.toString(),
    });
    
    const response = await authenticatedFetch(`${API_BASE_URL}/users/suggestions?${params}`);
    const result = await response.json();
    console.log('✅ User suggestions:', result.length, 'suggestions retrieved');
    return result;
  },

  getUserById: async (userId: string): Promise<User> => {
    const response = await authenticatedFetch(`${API_BASE_URL}/users/${userId}`);
    return response.json();
  },
};

// Conversation API functions
export const conversationApi = {
  getUserConversations: async (): Promise<Conversation[]> => {
    console.log('📋 Getting user conversations');
    
    const response = await authenticatedFetch(`${API_BASE_URL}/conversations`);
    const result = await response.json();
    console.log('✅ User conversations retrieved:', result.length, 'conversations');
    return result;
  },

  getConversationById: async (conversationId: string): Promise<Conversation> => {
    const response = await authenticatedFetch(`${API_BASE_URL}/conversations/${conversationId}`);
    return response.json();
  },

  createDirectConversation: async (participantId: string): Promise<Conversation> => {
    console.log('🔄 Creating direct conversation with participant:', participantId);
    
    const params = new URLSearchParams({
      participantId,
    });
    
    const url = `${API_BASE_URL}/conversations/direct?${params}`;
    
    const response = await authenticatedFetch(url, {
      method: 'POST',
    });
    
    const result = await response.json();
    console.log('✅ Direct conversation created:', result);
    
    return result;
  },

  addParticipant: async (conversationId: string, participantId: string): Promise<void> => {
    const params = new URLSearchParams({
      participantId,
    });
    
    await authenticatedFetch(`${API_BASE_URL}/conversations/${conversationId}/participants?${params}`, {
      method: 'POST',
    });
  },

  removeParticipant: async (conversationId: string, participantId: string): Promise<void> => {
    await authenticatedFetch(`${API_BASE_URL}/conversations/${conversationId}/participants/${participantId}`, {
      method: 'DELETE',
    });
  },
};

// Message search API functions
export interface MessageSearchResult {
  id: string;
  conversationId: string;
  senderId: string;
  senderUsername: string;
  content: string;
  highlightedContent?: string;
  timestamp: string;
  score?: number;
}

export interface SearchResult {
  messages: MessageSearchResult[];
  totalCount: number;
  currentPage: number;
  pageSize: number;
  hasMore: boolean;
  query: string;
  conversationId: string;
  nextPage?: number;
}

export const messageSearchApi = {
  searchMessages: async (
    conversationId: string, 
    query: string, 
    page = 0, 
    size = 20
  ): Promise<SearchResult> => {
    const params = new URLSearchParams({
      query,
      page: page.toString(),
      size: size.toString(),
    });
    
    const response = await authenticatedFetch(
      `${API_BASE_URL}/conversations/${conversationId}/search?${params}`
    );
    return response.json();
  },

  getMessageContext: async (
    conversationId: string,
    messageId: string,
    contextSize = 10
  ): Promise<MessageSearchResult[]> => {
    const params = new URLSearchParams({
      contextSize: contextSize.toString(),
    });
    
    const response = await authenticatedFetch(
      `${API_BASE_URL}/conversations/${conversationId}/search/messages/${messageId}/context?${params}`
    );
    return response.json();
  },
};

// Export all APIs
export const api = {
  user: userApi,
  conversation: conversationApi,
  messageSearch: messageSearchApi,
};

export default api;
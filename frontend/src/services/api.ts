import { User, Conversation } from '../types/chat';
import { tokenStorage } from '../utils/secureStorage';

// Get API base URL from runtime config or environment variables
const getApiBaseUrl = (): string => {
  // Debug: log the runtime environment
  if (typeof window !== 'undefined') {
    console.log('🔍 Runtime env object:', (window as any)._env_);
    console.log('🔍 Build-time env:', process.env.REACT_APP_API_BASE_URL);
  }
  
  // Check for runtime configuration first (Docker)
  if (typeof window !== 'undefined' && (window as any)._env_?.REACT_APP_API_BASE_URL) {
    const baseUrl = (window as any)._env_.REACT_APP_API_BASE_URL;
    console.log('🌐 Using runtime API base URL:', baseUrl);
    return `${baseUrl}/api`;
  }
  
  // Fall back to build-time environment variable if it's not empty
  const buildTimeUrl = process.env.REACT_APP_API_BASE_URL;
  if (buildTimeUrl && buildTimeUrl.trim()) {
    console.log('🌐 Using build-time API base URL:', buildTimeUrl);
    return `${buildTimeUrl}/api`;
  }
  
  // Final fallback - use relative path for single service deployment
  const fallbackUrl = '';
  console.log('🌐 Using relative API base URL for single service deployment');
  return '/api';
};

const API_BASE_URL = getApiBaseUrl();

// Helper function to get auth token from secure storage
const getAuthToken = (): string | null => {
  return tokenStorage.get();
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
      
      // Try to extract error message from response body
      let errorMessage = `HTTP error! status: ${response.status}`;
      let errorData = {};
      
      try {
        errorData = await response.json();
        if ((errorData as any).message) {
          errorMessage = (errorData as any).message;
        }
      } catch (parseError) {
        // If we can't parse the response body, use the generic error message
      }
      
      const error = new Error(errorMessage);
      (error as any).status = response.status;
      (error as any).response = { data: errorData };
      throw error;
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
  getAllUsers: async (): Promise<User[]> => {
    console.log('👥 Getting all users');
    
    try {
      const response = await authenticatedFetch(`${API_BASE_URL}/users`);
      const result = await response.json();
      
      // Handle both MessageResponse wrapper and direct array response
      if (Array.isArray(result)) {
        console.log('✅ All users retrieved:', result.length, 'users');
        return result;
      } else if (result.data && Array.isArray(result.data)) {
        console.log('✅ All users retrieved:', result.data.length, 'users');
        return result.data;
      } else {
        console.log('✅ All users retrieved:', result.length || 0, 'users');
        return result;
      }
    } catch (error) {
      console.error('Failed to fetch users:', error);
      throw error;
    }
  },

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

  createGroup: async (createGroupRequest: any): Promise<any> => {
    console.log('🔄 Creating group:', createGroupRequest);
    
    const response = await authenticatedFetch(`${API_BASE_URL}/conversations/groups`, {
      method: 'POST',
      body: JSON.stringify(createGroupRequest),
    });
    
    const result = await response.json();
    console.log('✅ Group created:', result);
    
    return result;
  },

  updateGroupSettings: async (conversationId: string, updateRequest: any): Promise<any> => {
    console.log('🔄 Updating group settings:', conversationId, updateRequest);
    
    const response = await authenticatedFetch(`${API_BASE_URL}/conversations/${conversationId}/settings`, {
      method: 'PUT',
      body: JSON.stringify(updateRequest),
    });
    
    const result = await response.json();
    console.log('✅ Group settings updated:', result);
    
    return result;
  },

  deleteConversation: async (conversationId: string): Promise<void> => {
    console.log('🔄 Deleting conversation:', conversationId);
    
    await authenticatedFetch(`${API_BASE_URL}/conversations/${conversationId}`, {
      method: 'DELETE',
    });
    
    console.log('✅ Conversation deleted successfully:', conversationId);
  },

  // Legacy method for backward compatibility
  deleteGroup: async (conversationId: string): Promise<void> => {
    console.log('🔄 Deleting group (legacy):', conversationId);
    
    await authenticatedFetch(`${API_BASE_URL}/conversations/${conversationId}`, {
      method: 'DELETE',
    });
    
    console.log('✅ Group deleted successfully:', conversationId);
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

// General API client with common HTTP methods
export const apiClient = {
  get: async (url: string): Promise<any> => {
    const response = await authenticatedFetch(`${API_BASE_URL}${url}`);
    return { data: await response.json() };
  },

  post: async (url: string, data?: any): Promise<any> => {
    const response = await authenticatedFetch(`${API_BASE_URL}${url}`, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
    return { data: await response.json() };
  },

  put: async (url: string, data?: any): Promise<any> => {
    const response = await authenticatedFetch(`${API_BASE_URL}${url}`, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
    return { data: await response.json() };
  },

  delete: async (url: string): Promise<any> => {
    const response = await authenticatedFetch(`${API_BASE_URL}${url}`, {
      method: 'DELETE',
    });
    return { data: response.ok ? null : await response.json() };
  },
};

// Export all APIs
export const api = {
  user: userApi,
  conversation: conversationApi,
  messageSearch: messageSearchApi,
  // Include general HTTP methods for backward compatibility
  ...apiClient,
};

export default api;
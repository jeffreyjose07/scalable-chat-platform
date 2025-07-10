package com.chatplatform.service.interfaces;

import com.chatplatform.dto.ConversationDto;
import java.util.List;

/**
 * Service interface for conversation management operations.
 * Defines the contract for creating, managing, and accessing conversations.
 */
public interface ConversationService {
    
    /**
     * Create a direct conversation between two users
     * 
     * @param userId1 ID of first user
     * @param userId2 ID of second user
     * @return ConversationDto with conversation details
     * @throws com.chatplatform.exception.ValidationException if users are invalid
     */
    ConversationDto createDirectConversation(String userId1, String userId2);
    
    /**
     * Get all conversations for a user with last message and unread count
     * 
     * @param userId ID of the user
     * @return List of ConversationDto objects
     */
    List<ConversationDto> getUserConversations(String userId);
    
    /**
     * Get a specific conversation by ID
     * 
     * @param conversationId ID of the conversation
     * @param userId ID of requesting user (for access control)
     * @return ConversationDto if user has access
     * @throws com.chatplatform.exception.AccessDeniedException if user doesn't have access
     * @throws com.chatplatform.exception.ConversationNotFoundException if conversation doesn't exist
     */
    ConversationDto getConversationById(String conversationId, String userId);
    
    /**
     * Check if user has access to a conversation
     * 
     * @param userId ID of the user
     * @param conversationId ID of the conversation
     * @return true if user has access, false otherwise
     */
    boolean hasUserAccess(String userId, String conversationId);
    
    /**
     * Add a user to a conversation
     * 
     * @param conversationId ID of the conversation
     * @param userId ID of the user to add
     * @param requestingUserId ID of user making the request (for access control)
     * @throws com.chatplatform.exception.AccessDeniedException if requesting user doesn't have permission
     * @throws com.chatplatform.exception.ConversationNotFoundException if conversation doesn't exist
     */
    void addUserToConversation(String conversationId, String userId, String requestingUserId);
    
    /**
     * Remove a user from a conversation
     * 
     * @param conversationId ID of the conversation
     * @param userId ID of the user to remove
     * @param requestingUserId ID of user making the request (for access control)
     * @throws com.chatplatform.exception.AccessDeniedException if requesting user doesn't have permission
     * @throws com.chatplatform.exception.ConversationNotFoundException if conversation doesn't exist
     */
    void removeUserFromConversation(String conversationId, String userId, String requestingUserId);
    
    /**
     * Update last read timestamp for a user in a conversation
     * 
     * @param conversationId ID of the conversation
     * @param userId ID of the user
     */
    void updateLastReadTimestamp(String conversationId, String userId);
    
    /**
     * Get conversation participants
     * 
     * @param conversationId ID of the conversation
     * @param requestingUserId ID of user making the request (for access control)
     * @return List of user IDs who are participants
     * @throws com.chatplatform.exception.AccessDeniedException if requesting user doesn't have access
     */
    List<String> getConversationParticipants(String conversationId, String requestingUserId);
}
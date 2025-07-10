package com.chatplatform.service.interfaces;

import com.chatplatform.dto.UserDto;
import java.util.List;

/**
 * Service interface for user search and discovery operations.
 * Defines the contract for finding and discovering users in the platform.
 */
public interface UserSearchService {
    
    /**
     * Search users by username or email
     * 
     * @param query search query string
     * @param currentUserId ID of user performing search (excluded from results)
     * @param limit maximum number of results to return
     * @return List of UserDto objects matching the search criteria
     */
    List<UserDto> searchUsers(String query, String currentUserId, int limit);
    
    /**
     * Get user suggestions for discovery
     * 
     * @param currentUserId ID of user requesting suggestions (excluded from results)
     * @param limit maximum number of suggestions to return
     * @return List of UserDto objects for user discovery
     */
    List<UserDto> getUserSuggestions(String currentUserId, int limit);
    
    /**
     * Get user by ID
     * 
     * @param userId ID of the user to retrieve
     * @return UserDto if user exists
     * @throws com.chatplatform.exception.UserNotFoundException if user doesn't exist
     */
    UserDto getUserById(String userId);
    
    /**
     * Check if user exists
     * 
     * @param userId ID of the user to check
     * @return true if user exists, false otherwise
     */
    boolean userExists(String userId);
    
    /**
     * Search users by email (exact match)
     * 
     * @param email email to search for
     * @return UserDto if user with email exists, null otherwise
     */
    UserDto findUserByEmail(String email);
    
    /**
     * Search users by username (exact match)
     * 
     * @param username username to search for
     * @return UserDto if user with username exists, null otherwise
     */
    UserDto findUserByUsername(String username);
}
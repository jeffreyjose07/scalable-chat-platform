package com.chatplatform.service;

import com.chatplatform.dto.UserDto;
import com.chatplatform.model.User;
import com.chatplatform.repository.jpa.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserSearchService.class);
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 100;
    
    private final UserRepository userRepository;
    
    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Search for users by username, email, or display name
     * Excludes the current user from results
     */
    public List<UserDto> searchUsers(String query, String currentUserId, int limit) {
        logger.debug("Searching users with query: '{}', excluding user: {}, limit: {}", query, currentUserId, limit);
        
        if (query == null || query.trim().isEmpty()) {
            logger.debug("Empty search query provided");
            return List.of();
        }
        
        // Validate and limit the search limit
        int validatedLimit = Math.min(Math.max(1, limit), MAX_SEARCH_LIMIT);
        if (validatedLimit != limit) {
            logger.debug("Search limit adjusted from {} to {}", limit, validatedLimit);
        }
        
        String sanitizedQuery = sanitizeQuery(query);
        if (sanitizedQuery.isEmpty()) {
            logger.debug("Query became empty after sanitization");
            return List.of();
        }
        
        // Create pageable with sorting by username
        Pageable pageable = PageRequest.of(0, validatedLimit, Sort.by("username").ascending());
        
        // Search for users (excluding current user)
        List<User> users = userRepository.findUsersMatchingQuery(sanitizedQuery, currentUserId, pageable);
        
        logger.debug("Found {} users matching query '{}'", users.size(), sanitizedQuery);
        
        return users.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Search for users by username, email, or display name with default limit
     */
    public List<UserDto> searchUsers(String query, String currentUserId) {
        return searchUsers(query, currentUserId, DEFAULT_SEARCH_LIMIT);
    }
    
    /**
     * Get user suggestions for a specific user (recently active users, etc.)
     * This could be enhanced with more sophisticated recommendations
     */
    public List<UserDto> getUserSuggestions(String currentUserId, int limit) {
        logger.debug("Getting user suggestions for user: {}, limit: {}", currentUserId, limit);
        
        int validatedLimit = Math.min(Math.max(1, limit), MAX_SEARCH_LIMIT);
        Pageable pageable = PageRequest.of(0, validatedLimit, Sort.by("lastSeenAt").descending());
        
        // Get recently active users (excluding current user)
        List<User> users = userRepository.findActiveUsersExcluding(currentUserId, pageable);
        
        logger.debug("Found {} user suggestions", users.size());
        
        return users.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get user suggestions with default limit
     */
    public List<UserDto> getUserSuggestions(String currentUserId) {
        return getUserSuggestions(currentUserId, DEFAULT_SEARCH_LIMIT);
    }
    
    /**
     * Check if a user exists by ID
     */
    public boolean userExists(String userId) {
        return userRepository.existsById(userId);
    }
    
    /**
     * Get user by ID (for conversation participants, etc.)
     */
    public UserDto getUserById(String userId) {
        return userRepository.findById(userId)
            .map(this::convertToDto)
            .orElse(null);
    }
    
    // Helper methods
    
    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        
        // Remove leading/trailing whitespace and convert to lowercase
        String sanitized = query.trim().toLowerCase();
        
        // Remove special characters that might interfere with SQL queries
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9\\s@._-]", "");
        
        // Remove extra whitespace
        sanitized = sanitized.replaceAll("\\s+", " ");
        
        return sanitized;
    }
    
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getDisplayName()
        );
        
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setOnline(user.isOnline());
        dto.setLastSeenAt(user.getLastSeenAt());
        
        return dto;
    }
}
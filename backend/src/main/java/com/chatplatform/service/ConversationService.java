package com.chatplatform.service;

import com.chatplatform.dto.ConversationDto;
import com.chatplatform.dto.UserDto;
import com.chatplatform.model.Conversation;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.model.ConversationType;
import com.chatplatform.model.User;
import com.chatplatform.repository.jpa.ConversationRepository;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import com.chatplatform.repository.jpa.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConversationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    
    public ConversationService(ConversationRepository conversationRepository,
                             ConversationParticipantRepository participantRepository,
                             UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Create a direct conversation between two users
     * If conversation already exists, return the existing one
     */
    public ConversationDto createDirectConversation(String userId1, String userId2) {
        logger.info("Creating direct conversation between users: {} and {}", userId1, userId2);
        
        try {
            // Validate users exist
            logger.debug("Validating users exist - User1: {}, User2: {}", userId1, userId2);
            User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId1);
                    return new IllegalArgumentException("User not found: " + userId1);
                });
            User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId2);
                    return new IllegalArgumentException("User not found: " + userId2);
                });
            
            logger.debug("Both users found - User1: {} ({}), User2: {} ({})", 
                        user1.getId(), user1.getUsername(), user2.getId(), user2.getUsername());
            
            // Check if direct conversation already exists
            logger.debug("Checking for existing direct conversation between users: {} and {}", userId1, userId2);
            Optional<Conversation> existingConversation = conversationRepository
                .findDirectConversationBetweenUsers(userId1, userId2);
            
            if (existingConversation.isPresent()) {
                logger.info("Direct conversation already exists: {}", existingConversation.get().getId());
                return convertToDto(existingConversation.get());
            }
            
            // Create standardized conversation ID (smaller ID first for consistency)
            String conversationId = createDirectConversationId(userId1, userId2);
            logger.debug("Generated conversation ID: {}", conversationId);
            
            // Create conversation
            logger.debug("Creating conversation entity - ID: {}, Type: DIRECT, CreatedBy: {}", 
                        conversationId, userId1);
            Conversation conversation = new Conversation(conversationId, ConversationType.DIRECT, null, userId1);
            conversationRepository.save(conversation);
            logger.debug("Conversation entity saved successfully: {}", conversationId);
            
            // Add both users as participants
            logger.debug("Adding participants - ConversationId: {}, User1: {}, User2: {}", 
                        conversationId, userId1, userId2);
            ConversationParticipant participant1 = new ConversationParticipant(conversation, user1);
            ConversationParticipant participant2 = new ConversationParticipant(conversation, user2);
            
            participantRepository.save(participant1);
            logger.debug("Participant1 saved successfully - ConversationId: {}, UserId: {}", 
                        conversationId, userId1);
            
            participantRepository.save(participant2);
            logger.debug("Participant2 saved successfully - ConversationId: {}, UserId: {}", 
                        conversationId, userId2);
            
            logger.info("Created direct conversation: {} between {} and {}", conversationId, userId1, userId2);
            
            return convertToDto(conversation);
            
        } catch (Exception e) {
            logger.error("Failed to create direct conversation between users: {} and {} - Error: {}", 
                        userId1, userId2, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get all conversations for a user
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(String userId) {
        logger.debug("Getting conversations for user: {}", userId);
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        List<Conversation> conversations = conversationRepository.findByParticipantUserId(userId);
        
        return conversations.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Get conversations by type for a user
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversationsByType(String userId, ConversationType type) {
        logger.debug("Getting {} conversations for user: {}", type, userId);
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        List<Conversation> conversations = conversationRepository.findByTypeAndParticipantUserId(type, userId);
        
        return conversations.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if user has access to a conversation
     */
    @Transactional(readOnly = true)
    public boolean hasUserAccess(String userId, String conversationId) {
        return participantRepository.existsByIdConversationIdAndIdUserIdAndIsActiveTrue(conversationId, userId);
    }
    
    /**
     * Get conversation details if user has access
     */
    @Transactional(readOnly = true)
    public Optional<ConversationDto> getConversationForUser(String conversationId, String userId) {
        if (!hasUserAccess(userId, conversationId)) {
            logger.warn("User {} does not have access to conversation {}", userId, conversationId);
            return Optional.empty();
        }
        
        Optional<Conversation> conversation = conversationRepository.findById(conversationId);
        return conversation.map(this::convertToDto);
    }
    
    /**
     * Add user to conversation (for group conversations)
     */
    public void addUserToConversation(String conversationId, String userId) {
        logger.info("Adding user {} to conversation {}", userId, conversationId);
        
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        
        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException("Cannot add users to direct conversations");
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        // Check if user is already a participant
        Optional<ConversationParticipant> existingParticipant = participantRepository
            .findByIdConversationIdAndIdUserId(conversationId, userId);
        
        if (existingParticipant.isPresent()) {
            if (existingParticipant.get().getIsActive()) {
                logger.info("User {} is already an active participant in conversation {}", userId, conversationId);
                return;
            } else {
                // Reactivate participant
                existingParticipant.get().setIsActive(true);
                participantRepository.save(existingParticipant.get());
                logger.info("Reactivated user {} in conversation {}", userId, conversationId);
                return;
            }
        }
        
        // Add new participant
        ConversationParticipant participant = new ConversationParticipant(conversationId, userId);
        participantRepository.save(participant);
        
        logger.info("Added user {} to conversation {}", userId, conversationId);
    }
    
    /**
     * Remove user from conversation (set as inactive)
     */
    public void removeUserFromConversation(String conversationId, String userId) {
        logger.info("Removing user {} from conversation {}", userId, conversationId);
        
        Optional<ConversationParticipant> participant = participantRepository
            .findByIdConversationIdAndIdUserId(conversationId, userId);
        
        if (participant.isPresent()) {
            participant.get().setIsActive(false);
            participantRepository.save(participant.get());
            logger.info("Removed user {} from conversation {}", userId, conversationId);
        } else {
            logger.warn("User {} is not a participant in conversation {}", userId, conversationId);
        }
    }
    
    // Helper methods
    
    private String createDirectConversationId(String userId1, String userId2) {
        // Ensure consistent ordering for conversation ID
        String smaller = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        String larger = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        return "dm_" + smaller + "_" + larger;
    }
    
    private ConversationDto convertToDto(Conversation conversation) {
        ConversationDto dto = new ConversationDto(
            conversation.getId(),
            conversation.getType(),
            conversation.getName(),
            conversation.getCreatedBy(),
            conversation.getCreatedAt(),
            conversation.getUpdatedAt()
        );
        
        // Load participants
        List<ConversationParticipant> participants = participantRepository
            .findByIdConversationIdAndIsActiveTrue(conversation.getId());
        
        List<UserDto> participantDtos = participants.stream()
            .map(participant -> {
                Optional<User> user = userRepository.findById(participant.getUserId());
                return user.map(this::convertUserToDto).orElse(null);
            })
            .filter(userDto -> userDto != null)
            .collect(Collectors.toList());
        
        dto.setParticipants(participantDtos);
        
        // TODO: Add last message info and unread count
        // This will be implemented when we integrate with MessageService
        
        return dto;
    }
    
    private UserDto convertUserToDto(User user) {
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
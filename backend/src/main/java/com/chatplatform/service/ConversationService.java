package com.chatplatform.service;

import com.chatplatform.dto.ConversationDto;
import com.chatplatform.dto.ConversationParticipantDto;
import com.chatplatform.dto.CreateGroupRequest;
import com.chatplatform.dto.UpdateGroupSettingsRequest;
import com.chatplatform.dto.UserDto;
import com.chatplatform.model.Conversation;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.model.ConversationType;
import com.chatplatform.model.ParticipantRole;
import com.chatplatform.model.User;
import com.chatplatform.util.Constants;
import com.chatplatform.repository.jpa.ConversationRepository;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import com.chatplatform.repository.jpa.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConversationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    
    public ConversationService(ConversationRepository conversationRepository,
                             ConversationParticipantRepository participantRepository,
                             UserRepository userRepository,
                             MessageService messageService) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
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
                    return new IllegalArgumentException(Constants.USER_NOT_FOUND + userId1);
                });
            User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId2);
                    return new IllegalArgumentException(Constants.USER_NOT_FOUND + userId2);
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
     * Create a group conversation
     */
    public ConversationDto createGroup(String creatorId, CreateGroupRequest request) {
        logger.info("Creating group - CreatorId: {}, GroupName: {}, IsPublic: {}, MaxParticipants: {}", 
                   creatorId, request.getName(), request.getIsPublic(), request.getMaxParticipants());
        
        try {
            // Validate creator exists
            User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> {
                    logger.error("Creator not found: {}", creatorId);
                    return new IllegalArgumentException(Constants.CREATOR_NOT_FOUND + creatorId);
                });
            
            // Generate unique group ID
            String groupId = "grp_" + UUID.randomUUID().toString().replace("-", "");
            logger.debug("Generated group ID: {}", groupId);
            
            // Create conversation with group metadata
            Conversation conversation = new Conversation(groupId, ConversationType.GROUP, request.getName(), creatorId);
            conversation.setDescription(request.getDescription());
            conversation.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
            conversation.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 100);
            
            conversationRepository.save(conversation);
            logger.debug("Group conversation saved successfully: {}", groupId);
            
            // Add creator as OWNER
            ConversationParticipant creatorParticipant = new ConversationParticipant(conversation, creator, ParticipantRole.OWNER);
            participantRepository.save(creatorParticipant);
            logger.debug("Creator added as OWNER - GroupId: {}, CreatorId: {}", groupId, creatorId);
            
            // Add initial participants as MEMBERS if provided
            if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
                for (String participantId : request.getParticipantIds()) {
                    if (!participantId.equals(creatorId)) {
                        User participant = userRepository.findById(participantId)
                            .orElseThrow(() -> {
                                logger.error("Participant not found: {}", participantId);
                                return new IllegalArgumentException(Constants.PARTICIPANT_NOT_FOUND + participantId);
                            });
                        
                        ConversationParticipant groupParticipant = new ConversationParticipant(conversation, participant, ParticipantRole.MEMBER);
                        participantRepository.save(groupParticipant);
                        logger.debug("Participant added as MEMBER - GroupId: {}, ParticipantId: {}", groupId, participantId);
                    }
                }
            }
            
            logger.info("Group created successfully - GroupId: {}, CreatorId: {}, GroupName: {}, TotalParticipants: {}", 
                       groupId, creatorId, request.getName(), 
                       1 + (request.getParticipantIds() != null ? request.getParticipantIds().size() : 0));
            
            return convertToDto(conversation);
            
        } catch (Exception e) {
            logger.error("Failed to create group - CreatorId: {}, GroupName: {}, Error: {}", 
                        creatorId, request.getName(), e.getMessage(), e);
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
            throw new IllegalArgumentException(Constants.USER_NOT_FOUND + userId);
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
            throw new IllegalArgumentException(Constants.USER_NOT_FOUND + userId);
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
     * Check if user can manage participants in a conversation
     */
    @Transactional(readOnly = true)
    public boolean canManageParticipants(String userId, String conversationId) {
        Optional<ConversationParticipant> participant = participantRepository
            .findByIdConversationIdAndIdUserIdAndIsActiveTrue(conversationId, userId);
        
        return participant.isPresent() && participant.get().canManageParticipants();
    }
    
    /**
     * Check if user can update conversation settings
     */
    @Transactional(readOnly = true)
    public boolean canUpdateSettings(String userId, String conversationId) {
        Optional<ConversationParticipant> participant = participantRepository
            .findByIdConversationIdAndIdUserIdAndIsActiveTrue(conversationId, userId);
        
        return participant.isPresent() && participant.get().canUpdateSettings();
    }
    
    /**
     * Check if user is owner of a conversation
     */
    @Transactional(readOnly = true)
    public boolean isOwner(String userId, String conversationId) {
        Optional<ConversationParticipant> participant = participantRepository
            .findByIdConversationIdAndIdUserIdAndIsActiveTrue(conversationId, userId);
        
        return participant.isPresent() && participant.get().isOwner();
    }
    
    /**
     * Get user's role in a conversation
     */
    @Transactional(readOnly = true)
    public Optional<ParticipantRole> getUserRole(String userId, String conversationId) {
        Optional<ConversationParticipant> participant = participantRepository
            .findByIdConversationIdAndIdUserIdAndIsActiveTrue(conversationId, userId);
        
        return participant.map(ConversationParticipant::getRole);
    }
    
    /**
     * Update group settings
     */
    public ConversationDto updateGroupSettings(String conversationId, UpdateGroupSettingsRequest request) {
        logger.info("Updating group settings - ConversationId: {}", conversationId);
        
        try {
            // Validate conversation exists and is a group
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    logger.error("Conversation not found: {}", conversationId);
                    return new IllegalArgumentException("Conversation not found: " + conversationId);
                });
            
            if (conversation.getType() != ConversationType.GROUP) {
                logger.error("Cannot update settings for non-group conversation: {}", conversationId);
                throw new IllegalArgumentException(Constants.CANNOT_UPDATE_SETTINGS_FOR_NON_GROUP);
            }
            
            // Update only provided fields
            if (request.getName() != null) {
                conversation.setName(request.getName());
                logger.debug("Updated group name - ConversationId: {}, NewName: {}", conversationId, request.getName());
            }
            
            if (request.getDescription() != null) {
                conversation.setDescription(request.getDescription());
                logger.debug("Updated group description - ConversationId: {}", conversationId);
            }
            
            if (request.getIsPublic() != null) {
                conversation.setIsPublic(request.getIsPublic());
                logger.debug("Updated group visibility - ConversationId: {}, IsPublic: {}", conversationId, request.getIsPublic());
            }
            
            if (request.getMaxParticipants() != null) {
                conversation.setMaxParticipants(request.getMaxParticipants());
                logger.debug("Updated group max participants - ConversationId: {}, MaxParticipants: {}", 
                           conversationId, request.getMaxParticipants());
            }
            
            conversationRepository.save(conversation);
            logger.info("Group settings updated successfully - ConversationId: {}", conversationId);
            
            return convertToDto(conversation);
            
        } catch (Exception e) {
            logger.error("Failed to update group settings - ConversationId: {}, Error: {}", 
                        conversationId, e.getMessage(), e);
            throw e;
        }
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
            .orElseThrow(() -> new IllegalArgumentException(Constants.CONVERSATION_NOT_FOUND + conversationId));
        
        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalArgumentException(Constants.CANNOT_ADD_USERS_TO_DIRECT_CONVERSATIONS);
        }
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException(Constants.USER_NOT_FOUND + userId);
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
        
        // Add new participant - need to load actual entities for @MapsId to work
        Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (conversationOpt.isEmpty()) {
            throw new IllegalArgumentException(Constants.CONVERSATION_NOT_FOUND + conversationId);
        }
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException(Constants.USER_NOT_FOUND + userId);
        }
        
        ConversationParticipant participant = new ConversationParticipant(
            conversationOpt.get(), 
            userOpt.get(), 
            ParticipantRole.MEMBER
        );
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
    
    /**
     * Delete conversation (only for group owners or direct conversation participants)
     */
    public void deleteConversation(String conversationId, String userId) {
        logger.info("Attempting to delete conversation {} by user {}", conversationId, userId);
        
        try {
            // Validate conversation exists
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    logger.error("Conversation not found: {}", conversationId);
                    return new IllegalArgumentException("Conversation not found: " + conversationId);
                });
            
            // Check user permissions
            if (conversation.getType() == ConversationType.GROUP) {
                // For groups, only owners can delete
                if (!isOwner(userId, conversationId)) {
                    logger.error("User {} does not have permission to delete group {}", userId, conversationId);
                    throw new IllegalArgumentException(Constants.ONLY_GROUP_OWNERS_CAN_DELETE_GROUPS);
                }
            } else if (conversation.getType() == ConversationType.DIRECT) {
                // For direct conversations, any participant can delete the entire conversation
                if (!hasUserAccess(userId, conversationId)) {
                    logger.error("User {} does not have access to conversation {}", userId, conversationId);
                    throw new IllegalArgumentException(Constants.NO_ACCESS_TO_CONVERSATION);
                }
                logger.info("User {} is deleting direct conversation {}", userId, conversationId);
                // Continue with full deletion logic below
            }
            
            // Delete all messages in the conversation first
            messageService.deleteConversationMessages(conversationId);
            logger.debug("Deleted all messages from conversation {}", conversationId);
            
            // Delete all participants (both active and inactive)
            List<ConversationParticipant> participants = participantRepository
                .findByIdConversationId(conversationId);
            participantRepository.deleteAll(participants);
            logger.debug("Deleted {} participants from conversation {}", participants.size(), conversationId);
            
            // Delete conversation
            conversationRepository.delete(conversation);
            logger.info("Successfully deleted conversation {} by user {}", conversationId, userId);
            
        } catch (Exception e) {
            logger.error("Failed to delete conversation {} by user {} - Error: {}", 
                        conversationId, userId, e.getMessage(), e);
            throw e;
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
        
        // Set group metadata for GROUP conversations
        if (conversation.getType() == ConversationType.GROUP) {
            dto.setDescription(conversation.getDescription());
            dto.setIsPublic(conversation.getIsPublic());
            dto.setMaxParticipants(conversation.getMaxParticipants());
        }
        
        // Load participants with role information
        List<ConversationParticipant> participants = participantRepository
            .findByIdConversationIdAndIsActiveTrue(conversation.getId());
        
        List<ConversationParticipantDto> participantDtos = participants.stream()
            .map(participant -> {
                Optional<User> user = userRepository.findById(participant.getUserId());
                return user.map(u -> {
                    UserDto userDto = convertUserToDto(u);
                    return new ConversationParticipantDto(
                        userDto, 
                        participant.getRole(),
                        participant.getJoinedAt(),
                        participant.getLastReadAt()
                    );
                }).orElse(null);
            })
            .filter(participantDto -> participantDto != null)
            .collect(Collectors.toList());
        
        dto.setParticipants(participantDtos);
        
        // Note: Last message info and unread count integration with MessageService
        // is planned for future enhancement to provide richer conversation metadata
        
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
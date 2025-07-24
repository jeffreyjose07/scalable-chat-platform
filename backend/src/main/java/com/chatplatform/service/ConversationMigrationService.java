package com.chatplatform.service;

import com.chatplatform.model.Conversation;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.model.ConversationType;
import com.chatplatform.model.User;
import com.chatplatform.repository.jpa.ConversationRepository;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import com.chatplatform.repository.jpa.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationMigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationMigrationService.class);
    
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    
    public ConversationMigrationService(ConversationRepository conversationRepository,
                                      ConversationParticipantRepository participantRepository,
                                      UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingConversations() {
        logger.info("Starting conversation migration...");
        
        try {
            // Note: Removed automatic default group creation to allow users full control
            // Default groups will only be created if explicitly requested by users
            logger.info("Conversation migration completed successfully (no default groups created)");
        } catch (Exception e) {
            logger.error("Error during conversation migration", e);
        }
    }
    
    private void createDefaultGroupConversation(String id, String name) {
        Optional<Conversation> existingConversation = conversationRepository.findById(id);
        
        if (existingConversation.isPresent()) {
            logger.info("Conversation '{}' already exists, skipping creation", id);
            return;
        }
        
        logger.info("Creating default group conversation: {} ({})", name, id);
        
        // Find admin user to set as creator
        Optional<User> adminUser = userRepository.findByEmail("admin@chatplatform.com");
        String createdBy = adminUser.map(User::getId).orElse("system");
        
        // Create conversation
        Conversation conversation = new Conversation(id, ConversationType.GROUP, name, createdBy);
        conversationRepository.save(conversation);
        
        // Add all existing users as participants
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            try {
                ConversationParticipant participant = new ConversationParticipant(id, user.getId());
                participant.setConversation(conversation);
                participant.setUser(user);
                participantRepository.save(participant);
                
                logger.debug("Added user '{}' to conversation '{}'", user.getUsername(), id);
            } catch (Exception e) {
                logger.warn("Failed to add user '{}' to conversation '{}': {}", 
                    user.getUsername(), id, e.getMessage());
            }
        }
        
        logger.info("Successfully created conversation '{}' with {} participants", 
            id, allUsers.size());
    }
    
    @Transactional
    public void addUserToAllGroupConversations(String userId) {
        logger.info("Adding user '{}' to all group conversations", userId);
        
        List<Conversation> groupConversations = conversationRepository.findByTypeOrderByUpdatedAtDesc(ConversationType.GROUP);
        
        for (Conversation conversation : groupConversations) {
            try {
                // Check if user is already a participant
                boolean alreadyParticipant = participantRepository.existsByIdConversationIdAndIdUserIdAndIsActiveTrue(
                    conversation.getId(), userId);
                
                if (!alreadyParticipant) {
                    ConversationParticipant participant = new ConversationParticipant(conversation.getId(), userId);
                    participantRepository.save(participant);
                    
                    logger.debug("Added user '{}' to group conversation '{}'", userId, conversation.getId());
                }
            } catch (Exception e) {
                logger.warn("Failed to add user '{}' to group conversation '{}': {}", 
                    userId, conversation.getId(), e.getMessage());
            }
        }
    }
    
    @Transactional
    public void removeUserFromAllConversations(String userId) {
        logger.info("Removing user '{}' from all conversations", userId);
        
        List<ConversationParticipant> userParticipations = participantRepository.findByIdUserIdAndIsActiveTrue(userId);
        
        for (ConversationParticipant participant : userParticipations) {
            participant.setIsActive(false);
            participantRepository.save(participant);
        }
        
        logger.info("Removed user '{}' from {} conversations", userId, userParticipations.size());
    }
}
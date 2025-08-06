package com.chatplatform.service;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MessageMigrationService implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageMigrationService.class);
    
    private final ChatMessageRepository messageRepository;
    private final ConversationParticipantRepository participantRepository;
    
    public MessageMigrationService(ChatMessageRepository messageRepository,
                                 ConversationParticipantRepository participantRepository) {
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
    }
    
    @Override
    public void run(String... args) {
        // Only run migration if enabled
        String runMigration = System.getProperty("chat.migration.enabled", "false");
        if (!"true".equalsIgnoreCase(runMigration)) {
            logger.info("Message migration disabled. Set -Dchat.migration.enabled=true to enable.");
            return;
        }
        
        logger.info("Starting message migration for read receipts...");
        migrateExistingMessages();
        logger.info("Message migration completed.");
    }
    
    /**
     * Migrate existing messages to have proper read receipt status
     */
    public void migrateExistingMessages() {
        logger.info("Migrating existing messages to support read receipts...");
        
        try {
            // Get all messages that don't have status set (null or empty)
            List<ChatMessage> messagesToMigrate = messageRepository.findAll().stream()
                .filter(msg -> msg.getStatus() == null)
                .toList();
            
            logger.info("Found {} messages to migrate", messagesToMigrate.size());
            
            int migratedCount = 0;
            int batchSize = 100;
            
            for (int i = 0; i < messagesToMigrate.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, messagesToMigrate.size());
                List<ChatMessage> batch = messagesToMigrate.subList(i, endIndex);
                
                for (ChatMessage message : batch) {
                    migrateMessage(message);
                    migratedCount++;
                }
                
                // Save batch
                messageRepository.saveAll(batch);
                
                if (migratedCount % 1000 == 0) {
                    logger.info("Migrated {} messages...", migratedCount);
                }
            }
            
            logger.info("Successfully migrated {} messages", migratedCount);
            
        } catch (Exception e) {
            logger.error("Error during message migration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Migrate a single message to have proper read receipt status
     */
    private void migrateMessage(ChatMessage message) {
        try {
            // Set default status to SENT for existing messages
            message.setStatus( ChatMessage.MessageStatus.SENT);
            
            // Mark as read by all participants who have a lastReadAt timestamp
            // that is after this message's timestamp
            List<ConversationParticipant> participants = participantRepository
                .findByConversationId(message.getConversationId());
            
            for (ConversationParticipant participant : participants) {
                String userId = participant.getUserId();
                
                // Skip the sender
                if (userId.equals(message.getSenderId())) {
                    continue;
                }
                
                // If participant has lastReadAt and it's after message timestamp, mark as read
                Instant lastReadAt = participant.getLastReadAt();
                if (lastReadAt != null && lastReadAt.isAfter(message.getTimestamp())) {
                    message.markAsDeliveredTo(userId);
                    message.markAsReadBy(userId);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to migrate message {}: {}", message.getId(), e.getMessage());
        }
    }
    
    /**
     * Manual migration trigger for specific conversation
     */
    public void migrateConversationMessages(String conversationId) {
        logger.info("Migrating messages for conversation: {}", conversationId);
        
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        
        for (ChatMessage message : messages) {
            if (message.getStatus() == null) {
                migrateMessage(message);
            }
        }
        
        messageRepository.saveAll(messages);
        logger.info("Migrated {} messages for conversation {}", messages.size(), conversationId);
    }
    
    /**
     * Check if migration is needed
     */
    public boolean isMigrationNeeded() {
        // Check if there are any messages without status
        List<ChatMessage> unmigrated = messageRepository.findAll().stream()
            .filter(msg -> msg.getStatus() == null)
            .limit(1)
            .toList();
        
        return !unmigrated.isEmpty();
    }
}
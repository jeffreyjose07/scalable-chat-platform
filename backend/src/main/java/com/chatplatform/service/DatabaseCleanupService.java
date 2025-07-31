package com.chatplatform.service;

import com.chatplatform.repository.jpa.ConversationRepository;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for cleaning up deleted conversations and orphaned messages
 * Runs scheduled cleanup tasks to maintain database hygiene
 */
@Service
public class DatabaseCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupService.class);
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    /**
     * Scheduled cleanup task that runs every hour
     * Cleans up:
     * 1. Messages from conversations that no longer exist
     * 2. Old deleted conversations (if soft delete is implemented)
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 milliseconds)
    @Transactional
    public void performHourlyCleanup() {
        logger.info("Starting hourly database cleanup task");
        
        try {
            // Clean up orphaned messages
            int orphanedMessagesDeleted = cleanupOrphanedMessages();
            
            // Clean up old soft-deleted conversations (if applicable)
            int deletedConversationsRemoved = cleanupSoftDeletedConversations();
            
            logger.info("Hourly cleanup completed. Orphaned messages deleted: {}, " +
                       "Soft-deleted conversations removed: {}", 
                       orphanedMessagesDeleted, deletedConversationsRemoved);
                       
        } catch (Exception e) {
            logger.error("Error during hourly cleanup", e);
        }
    }
    
    /**
     * Clean up messages that belong to conversations that no longer exist
     * @return number of orphaned messages deleted
     */
    private int cleanupOrphanedMessages() {
        try {
            // Get all existing conversation IDs
            List<String> existingConversationIds = conversationRepository.findAllConversationIds();
            logger.info("Found {} active conversations for cleanup comparison", existingConversationIds.size());
            
            // Log first few conversation IDs for debugging
            if (!existingConversationIds.isEmpty()) {
                logger.debug("Sample active conversation IDs: {}", 
                    existingConversationIds.stream().limit(3).toList());
            }
            
            // Get total message count before deletion
            long totalMessagesBefore = chatMessageRepository.count();
            logger.info("Total messages in MongoDB before cleanup: {}", totalMessagesBefore);
            
            // Delete messages that don't belong to any existing conversation
            long deletedCount = chatMessageRepository.deleteByConversationIdNotIn(existingConversationIds);
            
            // Get total message count after deletion
            long totalMessagesAfter = chatMessageRepository.count();
            logger.info("Deleted {} orphaned messages. Messages before: {}, after: {}", 
                       deletedCount, totalMessagesBefore, totalMessagesAfter);
            
            return (int) deletedCount;
            
        } catch (Exception e) {
            logger.error("Error cleaning up orphaned messages", e);
            return 0;
        }
    }
    
    /**
     * Clean up conversations that have been soft-deleted for more than 30 days
     * Note: This assumes soft delete implementation with deletedAt field
     * @return number of soft-deleted conversations permanently removed
     */
    private int cleanupSoftDeletedConversations() {
        try {
            // If you implement soft delete with a deletedAt field, uncomment below:
            /*
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<Conversation> oldDeletedConversations = conversationRepository
                .findByDeletedAtIsNotNullAndDeletedAtBefore(thirtyDaysAgo);
            
            for (Conversation conversation : oldDeletedConversations) {
                // Delete associated messages first
                chatMessageRepository.deleteByConversationId(conversation.getId());
                // Then delete the conversation
                conversationRepository.delete(conversation);
            }
            
            logger.debug("Permanently deleted {} old soft-deleted conversations", 
                        oldDeletedConversations.size());
            return oldDeletedConversations.size();
            */
            
            // For now, since soft delete might not be implemented, return 0
            logger.debug("Soft delete cleanup skipped - not implemented");
            return 0;
            
        } catch (Exception e) {
            logger.error("Error cleaning up soft-deleted conversations", e);
            return 0;
        }
    }
    
    /**
     * Manual cleanup method that can be called on-demand
     * Useful for maintenance or testing
     */
    public void performManualCleanup() {
        logger.info("Starting manual database cleanup");
        performHourlyCleanup();
    }
    
    /**
     * Get cleanup statistics
     * @return cleanup statistics as a formatted string
     */
    public String getCleanupStats() {
        try {
            long totalMessages = chatMessageRepository.count();
            long totalConversations = conversationRepository.count();
            
            return String.format("Database stats - Messages: %d, Conversations: %d", 
                               totalMessages, totalConversations);
        } catch (Exception e) {
            logger.error("Error getting cleanup stats", e);
            return "Error retrieving stats";
        }
    }
}
package com.chatplatform.service;

import com.chatplatform.repository.jpa.ConversationRepository;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
     * Scheduled cleanup task that runs every 20 days
     * Cleans up:
     * 1. Messages from conversations that no longer exist
     * 2. Messages from soft-deleted conversations (immediate cleanup)
     * 3. Old deleted conversations (permanently remove after 30 days)
     */
    @Scheduled(fixedRate = 1728000000L) // Run every 20 days (20 * 24 * 60 * 60 * 1000 milliseconds)
    public void performScheduledCleanup() {
        logger.info("Starting scheduled database cleanup task (every 20 days)");
        executeCleanupTransactionally();
    }
    
    /**
     * Executes the cleanup operations within a transaction
     * Separated to avoid @Transactional issues when called from other methods in same class
     */
    @Transactional
    public void executeCleanupTransactionally() {
        try {
            // Clean up orphaned messages (messages from conversations that don't exist)
            int orphanedMessagesDeleted = cleanupOrphanedMessages();
            
            // Clean up messages from soft-deleted conversations (immediate cleanup)
            int softDeletedMessagesDeleted = cleanupSoftDeletedConversationMessages();
            
            // Clean up old soft-deleted conversations (permanently remove after 30 days)
            int deletedConversationsRemoved = cleanupSoftDeletedConversations();
            
            logger.info("Cleanup completed. Orphaned messages deleted: {}, " +
                       "Soft-deleted messages deleted: {}, Conversations permanently removed: {}", 
                       orphanedMessagesDeleted, softDeletedMessagesDeleted, deletedConversationsRemoved);
                       
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
    
    /**
     * Clean up messages that belong to conversations that no longer exist
     * @return number of orphaned messages deleted
     */
    private int cleanupOrphanedMessages() {
        try {
            // Get all active (non-deleted) conversation IDs
            List<String> existingConversationIds = conversationRepository.findAllActiveConversationIds();
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
     * Clean up messages from soft-deleted conversations (immediate cleanup)
     * @return number of messages deleted from soft-deleted conversations
     */
    private int cleanupSoftDeletedConversationMessages() {
        try {
            // Get all soft-deleted conversation IDs
            List<String> softDeletedConversationIds = conversationRepository.findAllSoftDeletedConversationIds();
            
            if (softDeletedConversationIds.isEmpty()) {
                logger.info("No soft-deleted conversations found, skipping message cleanup");
                return 0;
            }
            
            logger.info("Found {} soft-deleted conversations with messages to clean", softDeletedConversationIds.size());
            
            // Delete messages from soft-deleted conversations
            long deletedCount = chatMessageRepository.deleteByConversationIdIn(softDeletedConversationIds);
            
            logger.info("Deleted {} messages from {} soft-deleted conversations", 
                       deletedCount, softDeletedConversationIds.size());
            
            return (int) deletedCount;
            
        } catch (Exception e) {
            logger.error("Error cleaning up soft-deleted conversation messages", e);
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
            // Clean up conversations that have been soft-deleted for more than 30 days
            Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60); // 30 days in seconds
            List<com.chatplatform.model.Conversation> oldDeletedConversations = conversationRepository
                .findByDeletedAtIsNotNullAndDeletedAtBefore(thirtyDaysAgo);
            
            int deletedCount = 0;
            for (com.chatplatform.model.Conversation conversation : oldDeletedConversations) {
                logger.info("Permanently deleting soft-deleted conversation: {} (deleted on: {})", 
                           conversation.getId(), conversation.getDeletedAt());
                
                // Delete associated messages first
                chatMessageRepository.deleteByConversationId(conversation.getId());
                logger.debug("Deleted messages for conversation {}", conversation.getId());
                
                // Then delete the conversation permanently
                conversationRepository.delete(conversation);
                deletedCount++;
            }
            
            logger.info("Permanently deleted {} old soft-deleted conversations", deletedCount);
            return deletedCount;
            
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
        executeCleanupTransactionally();
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
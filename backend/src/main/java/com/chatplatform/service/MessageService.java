package com.chatplatform.service;

import com.chatplatform.dto.MessageDistributionEvent;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class MessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    
    private final ChatMessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ConversationParticipantRepository participantRepository;
    
    // In-memory queue to replace Kafka
    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();
    private final ExecutorService messageProcessor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "message-processor");
        t.setDaemon(true);
        return t;
    });
    
    public MessageService(ChatMessageRepository messageRepository,
                         ApplicationEventPublisher eventPublisher,
                         ConversationParticipantRepository participantRepository) {
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.participantRepository = participantRepository;
    }
    
    @PostConstruct
    public void startMessageProcessor() {
        messageProcessor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ChatMessage message = messageQueue.take(); // Blocks until message available
                    processMessageInternal(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("❌ Error processing message from queue: {}", e.getMessage());
                }
            }
        });
        logger.info("🚀 Started in-memory message processor");
    }
    
    @PreDestroy
    public void shutdown() {
        messageProcessor.shutdown();
        logger.info("🛑 Shutdown in-memory message processor");
    }
    
    @Async
    public void processMessage(ChatMessage message) {
        try {
            // Add to in-memory queue for processing
            messageQueue.offer(message);
            logger.info("📤 Message queued for processing: {}", message.getContent().substring(0, Math.min(50, message.getContent().length())));
        } catch (Exception e) {
            logger.error("❌ Failed to queue message: {}", e.getMessage());
            // Direct processing as fallback
            processMessageInternal(message);
        }
    }
    
    private void processMessageInternal(ChatMessage message) {
        try {
            // Initialize message status and set default status to SENT
            if (message.getStatus() == null) {
                message.setStatus(ChatMessage.MessageStatus.SENT);
            }
            
            // Initialize delivery tracking for active conversation participants
            try {
                List<ConversationParticipant> participants = participantRepository.findByIdConversationIdAndIsActiveTrue(message.getConversationId());
                
                for (ConversationParticipant participant : participants) {
                    String participantUserId = participant.getUserId();
                    
                    // Don't mark sender's own message as delivered to themselves
                    if (!participantUserId.equals(message.getSenderId())) {
                        message.markAsDeliveredTo(participantUserId);
                        logger.debug("Marked message {} as delivered to participant: {}", message.getId(), participantUserId);
                    }
                }
                logger.debug("Initialized delivery status for {} participants", participants.size());
            } catch (Exception e) {
                logger.warn("Failed to initialize delivery status for message {}: {}", message.getId(), e.getMessage());
                // Continue with message processing even if delivery initialization fails
            }
            
            ChatMessage savedMessage = messageRepository.save(message);
            logger.info("💾 Message saved: {}", savedMessage.getId());
            
            // Publish event directly (no Kafka)
            eventPublisher.publishEvent(new MessageDistributionEvent(savedMessage));
            logger.info("✅ Message {} distributed via in-memory event publishing", savedMessage.getId());
            
        } catch (Exception e) {
            logger.error("❌ Error processing message internally: {}", e.getMessage());
        }
    }
    
    public List<ChatMessage> getConversationMessages(String conversationId) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }
    
    public List<ChatMessage> getConversationMessagesSince(String conversationId, Instant timestamp) {
        return messageRepository.findByConversationIdAndTimestampAfterOrderByTimestampAsc(conversationId, timestamp);
    }
    
    public List<ChatMessage> getPendingMessages(String userId) {
        // Return recent messages from the last hour
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        return messageRepository.findByTimestampAfterOrderByTimestampAsc(oneHourAgo);
    }
    
    public List<ChatMessage> getRecentMessagesForUser(String userId) {
        // Return recent messages from the last 24 hours
        Instant oneDayAgo = Instant.now().minusSeconds(86400);
        return messageRepository.findByTimestampAfterOrderByTimestampAsc(oneDayAgo);
    }
    
    public void deleteConversationMessages(String conversationId) {
        try {
            // Get count before deletion for logging
            long messageCount = messageRepository.countByConversationId(conversationId);
            logger.info("Deleting {} messages for conversation: {}", messageCount, conversationId);
            
            // Perform deletion
            messageRepository.deleteByConversationId(conversationId);
            
            // Verify deletion
            long remainingCount = messageRepository.countByConversationId(conversationId);
            logger.info("Conversation {} message deletion completed. Remaining messages: {}", 
                       conversationId, remainingCount);
            
            if (remainingCount > 0) {
                logger.warn("Warning: {} messages still remain for conversation {} after deletion", 
                           remainingCount, conversationId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to delete messages for conversation {}", conversationId, e);
            throw e; // Re-throw to ensure conversation deletion fails if message deletion fails
        }
    }
    
    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
}
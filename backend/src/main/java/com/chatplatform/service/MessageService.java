package com.chatplatform.service;

import com.chatplatform.dto.MessageDistributionEvent;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import java.time.Instant;
import java.util.List;

@Service
public class MessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    
    private final ChatMessageRepository messageRepository;
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;
    
    public MessageService(ChatMessageRepository messageRepository,
                         KafkaTemplate<String, ChatMessage> kafkaTemplate,
                         ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.eventPublisher = eventPublisher;
    }
    
    @Async
    public void processMessage(ChatMessage message) {
        try {
            ChatMessage savedMessage = messageRepository.save(message);
            
            // Send to Kafka with callback handling
            CompletableFuture<SendResult<String, ChatMessage>> future = 
                kafkaTemplate.send("chat-messages", savedMessage);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("✅ Message sent to Kafka successfully: {} (partition: {}, offset: {})", 
                        savedMessage.getId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                } else {
                    logger.error("❌ Failed to send message {} to Kafka: {}", savedMessage.getId(), ex.getMessage());
                    logger.warn("🔄 Falling back to direct event publishing for message: {}", savedMessage.getId());
                    
                    // Fallback: publish event directly if Kafka fails
                    try {
                        eventPublisher.publishEvent(new MessageDistributionEvent(savedMessage));
                        logger.info("✅ Message {} distributed via direct event publishing", savedMessage.getId());
                    } catch (Exception eventEx) {
                        logger.error("❌ Failed to distribute message {} via direct event: {}", 
                            savedMessage.getId(), eventEx.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage());
            
            // Emergency fallback: try to save and publish directly
            try {
                if (message.getId() == null) {
                    ChatMessage savedMessage = messageRepository.save(message);
                    eventPublisher.publishEvent(new MessageDistributionEvent(savedMessage));
                    logger.warn("Message {} processed via emergency fallback", savedMessage.getId());
                }
            } catch (Exception fallbackEx) {
                logger.error("Emergency fallback failed for message: {}", fallbackEx.getMessage());
            }
        }
    }
    
    @KafkaListener(topics = "chat-messages", groupId = "chat-platform")
    public void handleMessageFromKafka(ChatMessage message) {
        try {
            logger.info("📨 Received message from Kafka: {} (content: {})", 
                message.getId(), message.getContent().substring(0, Math.min(50, message.getContent().length())));
            eventPublisher.publishEvent(new MessageDistributionEvent(message));
            logger.info("🚀 Published MessageDistributionEvent for message: {}", message.getId());
        } catch (Exception e) {
            logger.error("❌ Error handling message from Kafka: {}", e.getMessage());
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
        messageRepository.deleteByConversationId(conversationId);
    }
    
    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
}
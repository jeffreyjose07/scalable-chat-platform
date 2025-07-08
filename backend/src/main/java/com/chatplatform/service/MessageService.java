package com.chatplatform.service;

import com.chatplatform.dto.MessageDistributionEvent;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
            
            kafkaTemplate.send("chat-messages", savedMessage);
            
            logger.info("Message processed and sent to Kafka: {}", savedMessage.getId());
            
        } catch (Exception e) {
            logger.error("Error processing message", e);
        }
    }
    
    @KafkaListener(topics = "chat-messages", groupId = "chat-platform")
    public void handleMessageFromKafka(ChatMessage message) {
        try {
            eventPublisher.publishEvent(new MessageDistributionEvent(message));
        } catch (Exception e) {
            logger.error("Error handling message from Kafka", e);
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
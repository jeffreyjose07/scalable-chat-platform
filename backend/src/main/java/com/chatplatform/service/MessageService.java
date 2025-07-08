package com.chatplatform.service;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import com.chatplatform.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class MessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    
    private final ChatMessageRepository messageRepository;
    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;
    private final ConnectionManager connectionManager;
    private final ChatWebSocketHandler webSocketHandler;
    
    public MessageService(ChatMessageRepository messageRepository,
                         KafkaTemplate<String, ChatMessage> kafkaTemplate,
                         ConnectionManager connectionManager,
                         ChatWebSocketHandler webSocketHandler) {
        this.messageRepository = messageRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.connectionManager = connectionManager;
        this.webSocketHandler = webSocketHandler;
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
            distributeMessage(message);
        } catch (Exception e) {
            logger.error("Error handling message from Kafka", e);
        }
    }
    
    private void distributeMessage(ChatMessage message) {
        String conversationId = message.getConversationId();
        
        // For now, we'll broadcast to all connected users
        // In a real application, you'd get conversation participants
        Set<String> sessions = connectionManager.getSessionsForServer(getServerId());
        
        sessions.forEach(sessionId -> {
            try {
                webSocketHandler.sendMessageToSession(sessionId, message);
            } catch (Exception e) {
                logger.error("Error sending message to session: {}", sessionId, e);
            }
        });
    }
    
    public List<ChatMessage> getConversationMessages(String conversationId) {
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }
    
    public List<ChatMessage> getConversationMessagesSince(String conversationId, Instant timestamp) {
        return messageRepository.findByConversationIdAndTimestampAfterOrderByTimestampAsc(conversationId, timestamp);
    }
    
    public List<ChatMessage> getPendingMessages(String userId) {
        // For now, return empty list
        // In a real application, you'd return messages that were sent while user was offline
        return List.of();
    }
    
    public void deleteConversationMessages(String conversationId) {
        messageRepository.deleteByConversationId(conversationId);
    }
    
    private String getServerId() {
        return System.getenv().getOrDefault("SERVER_ID", "server-1");
    }
}
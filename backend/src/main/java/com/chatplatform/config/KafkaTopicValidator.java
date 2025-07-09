package com.chatplatform.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaTopicValidator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicValidator.class);
    
    @Autowired
    private KafkaAdmin kafkaAdmin;
    
    @Autowired
    private NewTopic chatMessagesTopic;

    @Override
    public void run(String... args) throws Exception {
        // Add delay to ensure Kafka is ready
        Thread.sleep(2000);
        validateAndCreateTopics();
    }

    private void validateAndCreateTopics() {
        try {
            logger.info("Validating Kafka topics...");
            
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                // Check if topics exist
                ListTopicsResult topicsResult = adminClient.listTopics();
                Set<String> existingTopics = topicsResult.names().get(30, TimeUnit.SECONDS);
                
                logger.info("Existing Kafka topics: {}", existingTopics);
                
                if (!existingTopics.contains("chat-messages")) {
                    logger.warn("Topic 'chat-messages' not found. Creating...");
                    adminClient.createTopics(Arrays.asList(chatMessagesTopic)).all().get(30, TimeUnit.SECONDS);
                    logger.info("Successfully created topic 'chat-messages'");
                } else {
                    logger.info("Topic 'chat-messages' already exists");
                }
                
                // Verify topic creation
                Set<String> updatedTopics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
                if (updatedTopics.contains("chat-messages")) {
                    logger.info("✅ Kafka topic validation successful");
                } else {
                    logger.error("❌ Failed to create or validate 'chat-messages' topic");
                }
            }
            
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("❌ Failed to validate Kafka topics. Message delivery may fail.", e);
            logger.error("Please ensure Kafka is running and accessible at: {}", 
                kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"));
        }
    }
}
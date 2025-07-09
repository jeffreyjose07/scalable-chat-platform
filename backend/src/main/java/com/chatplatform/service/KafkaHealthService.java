package com.chatplatform.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaHealthService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthService.class);
    public static final String CHAT_MESSAGES = "chat-messages";

    @Autowired
    private KafkaAdmin kafkaAdmin;
    
    @Autowired
    private NewTopic chatMessagesTopic;
    
    private volatile boolean kafkaHealthy = false;
    private volatile boolean topicExists = false;

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    public void performHealthCheck() {
        checkKafkaHealth();
    }

    public void checkKafkaHealth() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // Check if Kafka is responsive
            ListTopicsResult topicsResult = adminClient.listTopics();
            Set<String> existingTopics = topicsResult.names().get(10, TimeUnit.SECONDS);
            
            kafkaHealthy = true;
            topicExists = existingTopics.contains(CHAT_MESSAGES);
            
            if (!topicExists) {
                logger.warn("Topic 'chat-messages' not found. Attempting to create...");
                createTopic(adminClient);
            }
            
            // Verify topic details
            if (topicExists) {
                verifyTopicConfiguration(adminClient);
            }
            
        } catch (Exception e) {
            kafkaHealthy = false;
            topicExists = false;
            logger.error("Kafka health check failed: {}", e.getMessage());
        }
    }

    private void createTopic(AdminClient adminClient) {
        try {
            adminClient.createTopics(Arrays.asList(chatMessagesTopic)).all().get(30, TimeUnit.SECONDS);
            logger.info("Successfully created topic 'chat-messages'");
            topicExists = true;
        } catch (Exception e) {
            logger.error("Failed to create topic 'chat-messages': {}", e.getMessage());
        }
    }

    private void verifyTopicConfiguration(AdminClient adminClient) {
        try {
            DescribeTopicsResult result = adminClient.describeTopics(Arrays.asList(CHAT_MESSAGES));
            Map<String, TopicDescription> descriptions = result.all().get(10, TimeUnit.SECONDS);
            
            TopicDescription desc = descriptions.get(CHAT_MESSAGES);
            if (desc != null) {
                logger.debug("Topic 'chat-messages' - Partitions: {}, Replication Factor: {}", 
                    desc.partitions().size(), 
                    desc.partitions().get(0).replicas().size());
            }
        } catch (Exception e) {
            logger.warn("Failed to verify topic configuration: {}", e.getMessage());
        }
    }

    public boolean isKafkaHealthy() {
        return kafkaHealthy;
    }

    public boolean isTopicExists() {
        return topicExists;
    }

    public String getHealthStatus() {
        if (!kafkaHealthy) {
            return "Kafka is not accessible";
        }
        if (!topicExists) {
            return "Topic 'chat-messages' does not exist";
        }
        return "Kafka is healthy";
    }
}
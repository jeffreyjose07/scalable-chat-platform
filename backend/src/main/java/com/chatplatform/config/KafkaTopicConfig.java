package com.chatplatform.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class KafkaTopicConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicConfig.class);
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name("chat-messages")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @PostConstruct
    public void initTopics() {
        logger.info("🔧 Initializing Kafka topics configuration...");
        logger.info("📍 Kafka bootstrap servers: {}", bootstrapServers);
        logger.info("📝 Chat messages topic will be created with 3 partitions and 1 replica");
    }
}
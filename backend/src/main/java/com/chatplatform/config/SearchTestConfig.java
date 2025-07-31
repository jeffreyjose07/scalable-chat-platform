package com.chatplatform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for search testing without full application dependencies
 */
@Configuration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "search-test")
public class SearchTestConfig {
    
    /**
     * Mock conversation service for search testing
     */
    @Bean
    @Primary
    public MockConversationService mockConversationService() {
        return new MockConversationService();
    }
    
    /**
     * Simple mock implementation that allows all access for testing
     */
    public static class MockConversationService {
        public boolean hasUserAccess(String userId, String conversationId) {
            // For testing purposes, allow all access
            return true;
        }
    }
}
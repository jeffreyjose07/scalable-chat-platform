package com.chatplatform.controller;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Simple test controller for debugging search functionality
 */
@RestController
@RequestMapping("/api/test")
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "search-test")
public class SearchTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchTestController.class);
    
    @Autowired
    private ChatMessageRepository messageRepository;
    
    /**
     * Test MongoDB text search directly
     */
    @GetMapping("/search")
    public List<ChatMessage> testSearch(
            @RequestParam String conversationId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("=== SEARCH TEST ===");
        logger.debug("ConversationId: {}, Query: {}, Page: {}, Size: {}", conversationId, query, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        try {
            List<ChatMessage> results = messageRepository.findByConversationIdAndTextSearch(conversationId, query, pageable);
            logger.debug("Found {} results", results.size());
            
            if (logger.isDebugEnabled()) {
                for (ChatMessage msg : results) {
                    logger.debug("- {}", msg.getContent());
                }
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Search error: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Test regex search as fallback
     */
    @GetMapping("/search-regex")
    public List<ChatMessage> testRegexSearch(
            @RequestParam String conversationId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.debug("=== REGEX SEARCH TEST ===");
        logger.debug("ConversationId: {}, Query: {}", conversationId, query);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        try {
            List<ChatMessage> results = messageRepository.findByConversationIdAndContentRegex(conversationId, query, pageable);
            logger.debug("Found {} results", results.size());
            
            if (logger.isDebugEnabled()) {
                for (ChatMessage msg : results) {
                    logger.debug("- {}", msg.getContent());
                }
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Regex search error: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * List all messages in a conversation
     */
    @GetMapping("/messages")
    public List<ChatMessage> listMessages(@RequestParam String conversationId) {
        logger.debug("=== LIST ALL MESSAGES ===");
        logger.debug("ConversationId: {}", conversationId);
        
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        logger.debug("Found {} total messages", messages.size());
        
        if (logger.isDebugEnabled()) {
            for (ChatMessage msg : messages) {
                logger.debug("- [{}] {}", msg.getSenderUsername(), msg.getContent());
            }
        }
        
        return messages;
    }
}
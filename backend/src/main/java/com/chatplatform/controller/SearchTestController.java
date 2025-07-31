package com.chatplatform.controller;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.repository.mongo.ChatMessageRepository;
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
        
        System.out.println("=== SEARCH TEST ===");
        System.out.println("ConversationId: " + conversationId);
        System.out.println("Query: " + query);
        System.out.println("Page: " + page + ", Size: " + size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        try {
            List<ChatMessage> results = messageRepository.findByConversationIdAndTextSearch(conversationId, query, pageable);
            System.out.println("Found " + results.size() + " results");
            
            for (ChatMessage msg : results) {
                System.out.println("- " + msg.getContent());
            }
            
            return results;
        } catch (Exception e) {
            System.err.println("Search error: " + e.getMessage());
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
        
        System.out.println("=== REGEX SEARCH TEST ===");
        System.out.println("ConversationId: " + conversationId);
        System.out.println("Query: " + query);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        try {
            List<ChatMessage> results = messageRepository.findByConversationIdAndContentRegex(conversationId, query, pageable);
            System.out.println("Found " + results.size() + " results");
            
            for (ChatMessage msg : results) {
                System.out.println("- " + msg.getContent());
            }
            
            return results;
        } catch (Exception e) {
            System.err.println("Regex search error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * List all messages in a conversation
     */
    @GetMapping("/messages")
    public List<ChatMessage> listMessages(@RequestParam String conversationId) {
        System.out.println("=== LIST ALL MESSAGES ===");
        System.out.println("ConversationId: " + conversationId);
        
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        System.out.println("Found " + messages.size() + " total messages");
        
        for (ChatMessage msg : messages) {
            System.out.println("- [" + msg.getSenderUsername() + "] " + msg.getContent());
        }
        
        return messages;
    }
}
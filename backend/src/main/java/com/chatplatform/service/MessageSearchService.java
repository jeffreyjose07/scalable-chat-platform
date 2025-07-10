package com.chatplatform.service;

import com.chatplatform.dto.MessageSearchResultDto;
import com.chatplatform.dto.SearchResultDto;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MessageSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageSearchService.class);
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int CONTEXT_WINDOW_SECONDS = 300; // 5 minutes
    
    private final ChatMessageRepository messageRepository;
    private final ConversationService conversationService;
    
    public MessageSearchService(ChatMessageRepository messageRepository, 
                              ConversationService conversationService) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
    }
    
    /**
     * Search messages within a conversation
     */
    public SearchResultDto searchMessages(String conversationId, String query, 
                                        String userId, int page, int size) {
        logger.debug("Searching messages in conversation: {}, query: '{}', user: {}, page: {}, size: {}", 
                    conversationId, query, userId, page, size);
        
        // Validate user access to conversation
        if (!conversationService.hasUserAccess(userId, conversationId)) {
            logger.warn("User {} does not have access to conversation {}", userId, conversationId);
            return createEmptyResult(query, conversationId, page, size);
        }
        
        // Validate and sanitize inputs
        String sanitizedQuery = sanitizeQuery(query);
        if (sanitizedQuery.isEmpty()) {
            logger.debug("Empty query after sanitization");
            return createEmptyResult(query, conversationId, page, size);
        }
        
        int validatedSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        int validatedPage = Math.max(0, page);
        
        // Create pageable
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, 
                                         Sort.by(Sort.Direction.DESC, "timestamp"));
        
        try {
            // Try MongoDB text search first
            return performTextSearch(conversationId, sanitizedQuery, validatedPage, validatedSize, pageable);
        } catch (Exception e) {
            logger.warn("Text search failed, falling back to regex search: {}", e.getMessage());
            // Fallback to regex search
            return performRegexSearch(conversationId, sanitizedQuery, validatedPage, validatedSize, pageable);
        }
    }
    
    /**
     * Search messages with default pagination
     */
    public SearchResultDto searchMessages(String conversationId, String query, String userId) {
        return searchMessages(conversationId, query, userId, 0, DEFAULT_PAGE_SIZE);
    }
    
    /**
     * Get messages around a specific message for context
     */
    public List<MessageSearchResultDto> getMessageContext(String messageId, String userId, int contextSize) {
        logger.debug("Getting context for message: {}, user: {}, contextSize: {}", messageId, userId, contextSize);
        
        // Get the target message
        ChatMessage targetMessage = messageRepository.findById(messageId).orElse(null);
        if (targetMessage == null) {
            logger.warn("Message not found: {}", messageId);
            return List.of();
        }
        
        // Validate user access to conversation
        if (!conversationService.hasUserAccess(userId, targetMessage.getConversationId())) {
            logger.warn("User {} does not have access to conversation {}", userId, targetMessage.getConversationId());
            return List.of();
        }
        
        // Get messages around the target message
        Instant targetTime = targetMessage.getTimestamp();
        Instant startTime = targetTime.minusSeconds(CONTEXT_WINDOW_SECONDS);
        Instant endTime = targetTime.plusSeconds(CONTEXT_WINDOW_SECONDS);
        
        List<ChatMessage> contextMessages = messageRepository.findByConversationIdAndTimestampBetween(
            targetMessage.getConversationId(), startTime, endTime);
        
        // Limit to contextSize messages (contextSize/2 before and after)
        int halfContext = contextSize / 2;
        List<ChatMessage> sortedMessages = contextMessages.stream()
            .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
            .collect(Collectors.toList());
        
        // Find the target message index
        int targetIndex = -1;
        for (int i = 0; i < sortedMessages.size(); i++) {
            if (sortedMessages.get(i).getId().equals(messageId)) {
                targetIndex = i;
                break;
            }
        }
        
        if (targetIndex == -1) {
            return List.of();
        }
        
        // Get context window
        int startIndex = Math.max(0, targetIndex - halfContext);
        int endIndex = Math.min(sortedMessages.size(), targetIndex + halfContext + 1);
        
        return sortedMessages.subList(startIndex, endIndex).stream()
            .map(msg -> convertToSearchResult(msg, null))
            .collect(Collectors.toList());
    }
    
    // Private helper methods
    
    private SearchResultDto performTextSearch(String conversationId, String query, 
                                            int page, int size, Pageable pageable) {
        logger.debug("Performing MongoDB text search for: '{}'", query);
        
        // Get search results
        List<ChatMessage> messages = messageRepository.findByConversationIdAndTextSearch(
            conversationId, query, pageable);
        
        // Get total count
        long totalCount = messageRepository.countByConversationIdAndTextSearch(conversationId, query);
        
        // Convert to DTOs with highlighting
        List<MessageSearchResultDto> resultDtos = messages.stream()
            .map(msg -> convertToSearchResult(msg, query))
            .collect(Collectors.toList());
        
        logger.debug("Text search found {} results out of {} total", resultDtos.size(), totalCount);
        
        return new SearchResultDto(resultDtos, (int) totalCount, page, size, query, conversationId);
    }
    
    private SearchResultDto performRegexSearch(String conversationId, String query, 
                                             int page, int size, Pageable pageable) {
        logger.debug("Performing regex search for: '{}'", query);
        
        // Escape special regex characters
        String escapedQuery = Pattern.quote(query);
        
        // Get search results
        List<ChatMessage> messages = messageRepository.findByConversationIdAndContentRegex(
            conversationId, escapedQuery, pageable);
        
        // Get total count
        long totalCount = messageRepository.countByConversationIdAndContentRegex(conversationId, escapedQuery);
        
        // Convert to DTOs with highlighting
        List<MessageSearchResultDto> resultDtos = messages.stream()
            .map(msg -> convertToSearchResult(msg, query))
            .collect(Collectors.toList());
        
        logger.debug("Regex search found {} results out of {} total", resultDtos.size(), totalCount);
        
        return new SearchResultDto(resultDtos, (int) totalCount, page, size, query, conversationId);
    }
    
    private MessageSearchResultDto convertToSearchResult(ChatMessage message, String query) {
        MessageSearchResultDto result = new MessageSearchResultDto(
            message.getId(),
            message.getConversationId(),
            message.getSenderId(),
            message.getSenderUsername(),
            message.getContent(),
            message.getTimestamp()
        );
        
        // Add highlighting if query is provided
        if (query != null && !query.isEmpty()) {
            result.setHighlightedContent(highlightText(message.getContent(), query));
        }
        
        return result;
    }
    
    private String highlightText(String text, String query) {
        if (text == null || query == null || query.isEmpty()) {
            return text;
        }
        
        try {
            // Simple highlighting - wrap matches in <mark> tags
            String escapedQuery = Pattern.quote(query);
            Pattern pattern = Pattern.compile(escapedQuery, Pattern.CASE_INSENSITIVE);
            return pattern.matcher(text).replaceAll("<mark>$0</mark>");
        } catch (Exception e) {
            logger.warn("Failed to highlight text: {}", e.getMessage());
            return text;
        }
    }
    
    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }
        
        // Remove leading/trailing whitespace
        String sanitized = query.trim();
        
        // Remove or escape potentially problematic characters for MongoDB text search
        sanitized = sanitized.replaceAll("[\"'\\\\]", "");
        
        // Limit length to prevent abuse
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        
        return sanitized;
    }
    
    private SearchResultDto createEmptyResult(String query, String conversationId, int page, int size) {
        return new SearchResultDto(List.of(), 0, page, size, query, conversationId);
    }
}
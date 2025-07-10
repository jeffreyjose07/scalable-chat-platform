package com.chatplatform.controller;

import com.chatplatform.dto.MessageSearchRequest;
import com.chatplatform.dto.MessageSearchResultDto;
import com.chatplatform.dto.SearchResultDto;
import com.chatplatform.service.MessageSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping("/api/conversations/{conversationId}/search")
public class MessageSearchController {
    
    private final MessageSearchService messageSearchService;
    
    public MessageSearchController(MessageSearchService messageSearchService) {
        this.messageSearchService = messageSearchService;
    }
    
    @GetMapping
    public ResponseEntity<SearchResultDto> searchMessages(
            @PathVariable String conversationId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication) {
        String userId = authentication.getName();
        SearchResultDto results = messageSearchService.searchMessages(conversationId, query, userId, page, size);
        return ResponseEntity.ok(results);
    }
    
    @PostMapping
    public ResponseEntity<SearchResultDto> searchMessagesPost(
            @PathVariable String conversationId,
            @Valid @RequestBody MessageSearchRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        SearchResultDto results = messageSearchService.searchMessages(
            conversationId, 
            request.getQuery(), 
            userId, 
            request.getPage(), 
            request.getSize()
        );
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/messages/{messageId}/context")
    public ResponseEntity<List<MessageSearchResultDto>> getMessageContext(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int contextSize,
            Authentication authentication) {
        String userId = authentication.getName();
        List<MessageSearchResultDto> context = messageSearchService.getMessageContext(messageId, userId, contextSize);
        return ResponseEntity.ok(context);
    }
}
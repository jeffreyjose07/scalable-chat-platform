package com.chatplatform.controller;

import com.chatplatform.dto.ConversationDto;
import com.chatplatform.dto.CreateConversationRequest;
import com.chatplatform.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    
    private final ConversationService conversationService;
    
    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }
    
    @PostMapping("/direct")
    public ResponseEntity<ConversationDto> createDirectConversation(
            @RequestParam String participantId,
            Authentication authentication) {
        String userId = authentication.getName();
        ConversationDto conversation = conversationService.createDirectConversation(userId, participantId);
        return ResponseEntity.ok(conversation);
    }
    
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        String userId = authentication.getName();
        Optional<ConversationDto> conversation = conversationService.getConversationForUser(conversationId, userId);
        return conversation.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<ConversationDto>> getUserConversations(Authentication authentication) {
        String userId = authentication.getName();
        List<ConversationDto> conversations = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }
    
    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable String conversationId,
            @RequestParam String participantId,
            Authentication authentication) {
        String userId = authentication.getName();
        // Check if current user has access to add participants
        if (!conversationService.hasUserAccess(userId, conversationId)) {
            return ResponseEntity.status(403).build();
        }
        conversationService.addUserToConversation(conversationId, participantId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{conversationId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable String conversationId,
            @PathVariable String participantId,
            Authentication authentication) {
        String userId = authentication.getName();
        // Check if current user has access to remove participants
        if (!conversationService.hasUserAccess(userId, conversationId)) {
            return ResponseEntity.status(403).build();
        }
        conversationService.removeUserFromConversation(conversationId, participantId);
        return ResponseEntity.ok().build();
    }
}
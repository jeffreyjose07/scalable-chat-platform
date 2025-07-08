package com.chatplatform.controller;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.User;
import com.chatplatform.service.MessageService;
import com.chatplatform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    
    private final MessageService messageService;
    private final UserService userService;
    
    public MessageController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }
    
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable String conversationId,
            Authentication authentication) {
        try {
            if (authentication == null) {
                logger.warn("Unauthenticated request to get conversation messages");
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Authentication required"));
            }
            
            logger.info("Fetching messages for conversation: {} by user: {}", 
                conversationId, authentication.getName());
            
            List<ChatMessage> messages = messageService.getConversationMessages(conversationId);
            
            logger.info("Found {} messages for conversation: {}", messages.size(), conversationId);
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            logger.error("Error fetching conversation messages for conversation: {}", conversationId, e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch messages: " + e.getMessage()));
        }
    }
    
    @GetMapping("/conversations/{conversationId}/since/{timestamp}")
    public ResponseEntity<?> getMessagesSince(
            @PathVariable String conversationId,
            @PathVariable String timestamp,
            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Authentication required"));
            }
            
            Instant since = Instant.parse(timestamp);
            List<ChatMessage> messages = messageService.getConversationMessagesSince(conversationId, since);
            
            logger.info("Found {} messages since {} for conversation: {}", 
                messages.size(), timestamp, conversationId);
            return ResponseEntity.ok(messages);
            
        } catch (DateTimeParseException e) {
            logger.error("Invalid timestamp format: {}", timestamp, e);
            return ResponseEntity.status(400)
                .body(Map.of("error", "Invalid timestamp format"));
        } catch (Exception e) {
            logger.error("Error fetching messages since {} for conversation: {}", timestamp, conversationId, e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch messages: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentMessages(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401)
                    .body(Map.of("error", "Authentication required"));
            }
            
            String username = authentication.getName();
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                logger.warn("User not found for username: {}", username);
                return ResponseEntity.status(404)
                    .body(Map.of("error", "User not found"));
            }
            
            String userId = userOpt.get().getId();
            List<ChatMessage> messages = messageService.getRecentMessagesForUser(userId);
            
            logger.info("Found {} recent messages for user: {} ({})", 
                messages.size(), username, userId);
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            logger.error("Error fetching recent messages for user: {}", 
                authentication != null ? authentication.getName() : "unknown", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch recent messages: " + e.getMessage()));
        }
    }
}
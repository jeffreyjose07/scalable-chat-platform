package com.chatplatform.controller;

import com.chatplatform.dto.ConversationDto;
import com.chatplatform.dto.CreateConversationRequest;
import com.chatplatform.dto.CreateGroupRequest;
import com.chatplatform.dto.UpdateGroupSettingsRequest;
import com.chatplatform.model.User;
import com.chatplatform.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);
    
    private final ConversationService conversationService;
    
    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }
    
    private String getUserId(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }
    
    @PostMapping("/direct")
    public ResponseEntity<ConversationDto> createDirectConversation(
            @RequestParam String participantId,
            Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Creating direct conversation - User: {}, Participant: {}", userId, participantId);
        
        try {
            ConversationDto conversation = conversationService.createDirectConversation(userId, participantId);
            logger.info("Direct conversation created successfully - ConversationId: {}, User: {}, Participant: {}", 
                       conversation.getId(), userId, participantId);
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            logger.error("Failed to create direct conversation - User: {}, Participant: {}, Error: {}", 
                        userId, participantId, e.getMessage(), e);
            throw e;
        }
    }
    
    @PostMapping("/groups")
    public ResponseEntity<ConversationDto> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Creating group - User: {}, GroupName: {}, IsPublic: {}, MaxParticipants: {}", 
                   userId, request.getName(), request.getIsPublic(), request.getMaxParticipants());
        
        try {
            ConversationDto conversation = conversationService.createGroup(userId, request);
            logger.info("Group created successfully - ConversationId: {}, User: {}, GroupName: {}", 
                       conversation.getId(), userId, request.getName());
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            logger.error("Failed to create group - User: {}, GroupName: {}, Error: {}", 
                        userId, request.getName(), e.getMessage(), e);
            throw e;
        }
    }
    
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Getting conversation - ConversationId: {}, User: {}", conversationId, userId);
        
        try {
            Optional<ConversationDto> conversation = conversationService.getConversationForUser(conversationId, userId);
            if (conversation.isPresent()) {
                logger.info("Conversation retrieved successfully - ConversationId: {}, User: {}", conversationId, userId);
                return ResponseEntity.ok(conversation.get());
            } else {
                logger.warn("Conversation not found or user doesn't have access - ConversationId: {}, User: {}", 
                           conversationId, userId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get conversation - ConversationId: {}, User: {}, Error: {}", 
                        conversationId, userId, e.getMessage(), e);
            throw e;
        }
    }
    
    @GetMapping
    public ResponseEntity<List<ConversationDto>> getUserConversations(Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Getting user conversations - User: {}", userId);
        
        try {
            List<ConversationDto> conversations = conversationService.getUserConversations(userId);
            logger.info("User conversations retrieved successfully - User: {}, Count: {}", userId, conversations.size());
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            logger.error("Failed to get user conversations - User: {}, Error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
    
    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable String conversationId,
            @RequestParam String participantId,
            Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Adding participant - ConversationId: {}, User: {}, Participant: {}", 
                   conversationId, userId, participantId);
        
        try {
            // Check if current user has permission to add participants
            if (!conversationService.canManageParticipants(userId, conversationId)) {
                logger.warn("User doesn't have permission to add participants - ConversationId: {}, User: {}", 
                           conversationId, userId);
                return ResponseEntity.status(403).build();
            }
            
            conversationService.addUserToConversation(conversationId, participantId);
            logger.info("Participant added successfully - ConversationId: {}, User: {}, Participant: {}", 
                       conversationId, userId, participantId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to add participant - ConversationId: {}, User: {}, Participant: {}, Error: {}", 
                        conversationId, userId, participantId, e.getMessage(), e);
            throw e;
        }
    }
    
    @DeleteMapping("/{conversationId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable String conversationId,
            @PathVariable String participantId,
            Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Removing participant - ConversationId: {}, User: {}, Participant: {}", 
                   conversationId, userId, participantId);
        
        try {
            // Check if current user has permission to remove participants
            if (!conversationService.canManageParticipants(userId, conversationId)) {
                logger.warn("User doesn't have permission to remove participants - ConversationId: {}, User: {}", 
                           conversationId, userId);
                return ResponseEntity.status(403).build();
            }
            
            conversationService.removeUserFromConversation(conversationId, participantId);
            logger.info("Participant removed successfully - ConversationId: {}, User: {}, Participant: {}", 
                       conversationId, userId, participantId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to remove participant - ConversationId: {}, User: {}, Participant: {}, Error: {}", 
                        conversationId, userId, participantId, e.getMessage(), e);
            throw e;
        }
    }
    
    @PutMapping("/{conversationId}/settings")
    public ResponseEntity<ConversationDto> updateGroupSettings(
            @PathVariable String conversationId,
            @Valid @RequestBody UpdateGroupSettingsRequest request,
            Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Updating group settings - ConversationId: {}, User: {}", conversationId, userId);
        
        try {
            // Check if current user has permission to update settings
            if (!conversationService.canUpdateSettings(userId, conversationId)) {
                logger.warn("User doesn't have permission to update group settings - ConversationId: {}, User: {}", 
                           conversationId, userId);
                return ResponseEntity.status(403).build();
            }
            
            ConversationDto updatedConversation = conversationService.updateGroupSettings(conversationId, request);
            logger.info("Group settings updated successfully - ConversationId: {}, User: {}", 
                       conversationId, userId);
            return ResponseEntity.ok(updatedConversation);
        } catch (Exception e) {
            logger.error("Failed to update group settings - ConversationId: {}, User: {}, Error: {}", 
                        conversationId, userId, e.getMessage(), e);
            throw e;
        }
    }
    
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String conversationId,
            Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Deleting conversation - ConversationId: {}, User: {}", conversationId, userId);
        
        try {
            conversationService.deleteConversation(conversationId, userId);
            logger.info("Conversation deleted successfully - ConversationId: {}, User: {}", 
                       conversationId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Delete conversation request denied - ConversationId: {}, User: {}, Reason: {}", 
                       conversationId, userId, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            logger.error("Failed to delete conversation - ConversationId: {}, User: {}, Error: {}", 
                        conversationId, userId, e.getMessage(), e);
            throw e;
        }
    }
}
package com.chatplatform.controller;

import com.chatplatform.model.ChatMessage;
import com.chatplatform.model.User;
import com.chatplatform.service.MessageService;
import com.chatplatform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MessageController messageController;

    private ChatMessage testMessage;
    private User testUser;

    @BeforeEach
    void setUp() {
        testMessage = new ChatMessage();
        testMessage.setId("msg1");
        testMessage.setContent("Test message");
        testMessage.setSenderId("user1");
        testMessage.setConversationId("conv1");
        testMessage.setTimestamp(Instant.now());

        testUser = new User();
        testUser.setId("user1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testGetConversationMessages_Success() {
        String conversationId = "conv1";
        List<ChatMessage> messages = Arrays.asList(testMessage);

        when(authentication.getName()).thenReturn("testuser");
        when(messageService.getConversationMessages(conversationId)).thenReturn(messages);

        ResponseEntity<?> response = messageController.getConversationMessages(conversationId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messages, response.getBody());
        verify(messageService).getConversationMessages(conversationId);
    }

    @Test
    void testGetConversationMessages_Unauthenticated() {
        String conversationId = "conv1";

        ResponseEntity<?> response = messageController.getConversationMessages(conversationId, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Authentication required", body.get("error"));
        verify(messageService, never()).getConversationMessages(any());
    }

    @Test
    void testGetConversationMessages_ServiceException() {
        String conversationId = "conv1";
        String errorMessage = "Service error";

        when(authentication.getName()).thenReturn("testuser");
        when(messageService.getConversationMessages(conversationId))
                .thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<?> response = messageController.getConversationMessages(conversationId, authentication);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.get("error").toString().contains(errorMessage));
    }

    @Test
    void testGetMessagesSince_Success() {
        String conversationId = "conv1";
        String timestamp = "2023-01-01T00:00:00Z";
        List<ChatMessage> messages = Arrays.asList(testMessage);

        when(messageService.getConversationMessagesSince(eq(conversationId), any(Instant.class)))
                .thenReturn(messages);

        ResponseEntity<?> response = messageController.getMessagesSince(conversationId, timestamp, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messages, response.getBody());
        verify(messageService).getConversationMessagesSince(eq(conversationId), any(Instant.class));
    }

    @Test
    void testGetMessagesSince_Unauthenticated() {
        String conversationId = "conv1";
        String timestamp = "2023-01-01T00:00:00Z";

        ResponseEntity<?> response = messageController.getMessagesSince(conversationId, timestamp, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Authentication required", body.get("error"));
        verify(messageService, never()).getConversationMessagesSince(any(), any());
    }

    @Test
    void testGetMessagesSince_InvalidTimestamp() {
        String conversationId = "conv1";
        String invalidTimestamp = "invalid-timestamp";

        ResponseEntity<?> response = messageController.getMessagesSince(conversationId, invalidTimestamp, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Invalid timestamp format", body.get("error"));
        verify(messageService, never()).getConversationMessagesSince(any(), any());
    }

    @Test
    void testGetMessagesSince_ServiceException() {
        String conversationId = "conv1";
        String timestamp = "2023-01-01T00:00:00Z";
        String errorMessage = "Service error";

        when(messageService.getConversationMessagesSince(eq(conversationId), any(Instant.class)))
                .thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<?> response = messageController.getMessagesSince(conversationId, timestamp, authentication);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.get("error").toString().contains(errorMessage));
    }

    @Test
    void testGetRecentMessages_Success() {
        String username = "testuser";
        List<ChatMessage> messages = Arrays.asList(testMessage);

        when(authentication.getName()).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(messageService.getRecentMessagesForUser(testUser.getId())).thenReturn(messages);

        ResponseEntity<?> response = messageController.getRecentMessages(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messages, response.getBody());
        verify(userService).findByUsername(username);
        verify(messageService).getRecentMessagesForUser(testUser.getId());
    }

    @Test
    void testGetRecentMessages_Unauthenticated() {
        ResponseEntity<?> response = messageController.getRecentMessages(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Authentication required", body.get("error"));
        verify(userService, never()).findByUsername(any());
        verify(messageService, never()).getRecentMessagesForUser(any());
    }

    @Test
    void testGetRecentMessages_UserNotFound() {
        String username = "testuser";

        when(authentication.getName()).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.empty());

        ResponseEntity<?> response = messageController.getRecentMessages(authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("User not found", body.get("error"));
        verify(userService).findByUsername(username);
        verify(messageService, never()).getRecentMessagesForUser(any());
    }

    @Test
    void testGetRecentMessages_ServiceException() {
        String username = "testuser";
        String errorMessage = "Service error";

        when(authentication.getName()).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(messageService.getRecentMessagesForUser(testUser.getId()))
                .thenThrow(new RuntimeException(errorMessage));

        ResponseEntity<?> response = messageController.getRecentMessages(authentication);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertTrue(body.get("error").toString().contains(errorMessage));
    }
}
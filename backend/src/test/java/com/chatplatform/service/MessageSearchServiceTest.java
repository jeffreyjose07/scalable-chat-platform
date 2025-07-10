package com.chatplatform.service;

import com.chatplatform.dto.MessageSearchResultDto;
import com.chatplatform.dto.SearchResultDto;
import com.chatplatform.model.ChatMessage;
import com.chatplatform.repository.mongo.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSearchServiceTest {
    
    @Mock
    private ChatMessageRepository messageRepository;
    
    @Mock
    private ConversationService conversationService;
    
    @InjectMocks
    private MessageSearchService messageSearchService;
    
    private ChatMessage message1;
    private ChatMessage message2;
    private ChatMessage message3;
    private String conversationId;
    private String userId;
    
    @BeforeEach
    void setUp() {
        conversationId = "conv123";
        userId = "user123";
        
        message1 = new ChatMessage(conversationId, "user1", "john", "Hello world", ChatMessage.MessageType.TEXT);
        message1.setId("msg1");
        message1.setTimestamp(Instant.now().minusSeconds(3600));
        
        message2 = new ChatMessage(conversationId, "user2", "jane", "Hello everyone", ChatMessage.MessageType.TEXT);
        message2.setId("msg2");
        message2.setTimestamp(Instant.now().minusSeconds(1800));
        
        message3 = new ChatMessage(conversationId, "user3", "bob", "Good morning", ChatMessage.MessageType.TEXT);
        message3.setId("msg3");
        message3.setTimestamp(Instant.now().minusSeconds(900));
    }
    
    @Test
    void testSearchMessages_WithValidAccess() {
        // Given
        String query = "hello";
        int page = 0;
        int size = 10;
        
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq(query), any(Pageable.class)))
            .thenReturn(Arrays.asList(message1, message2));
        when(messageRepository.countByConversationIdAndTextSearch(conversationId, query))
            .thenReturn(2L);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, page, size);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.getResultCount());
        assertEquals(2, result.getTotalCount());
        assertEquals(page, result.getCurrentPage());
        assertEquals(size, result.getPageSize());
        assertEquals(query, result.getQuery());
        assertEquals(conversationId, result.getConversationId());
        assertFalse(result.isHasMore());
        
        // Verify messages are converted correctly
        List<MessageSearchResultDto> messages = result.getMessages();
        assertEquals(2, messages.size());
        
        MessageSearchResultDto firstResult = messages.get(0);
        assertEquals(message1.getId(), firstResult.getId());
        assertEquals(message1.getContent(), firstResult.getContent());
        assertEquals(message1.getSenderUsername(), firstResult.getSenderUsername());
        
        // Verify highlighting is applied
        assertTrue(firstResult.getHighlightedContent().contains("<mark>"));
        
        verify(conversationService).hasUserAccess(userId, conversationId);
        verify(messageRepository).findByConversationIdAndTextSearch(
            eq(conversationId), 
            eq(query), 
            eq(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")))
        );
    }
    
    @Test
    void testSearchMessages_WithoutAccess() {
        // Given
        String query = "hello";
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(false);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, 0, 10);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalCount());
        assertEquals(query, result.getQuery());
        assertEquals(conversationId, result.getConversationId());
        
        verify(conversationService).hasUserAccess(userId, conversationId);
        verify(messageRepository, never()).findByConversationIdAndTextSearch(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void testSearchMessages_EmptyQuery() {
        // Given
        String query = "";
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, 0, 10);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalCount());
        
        verify(messageRepository, never()).findByConversationIdAndTextSearch(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void testSearchMessages_NullQuery() {
        // Given
        String query = null;
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, 0, 10);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalCount());
        
        verify(messageRepository, never()).findByConversationIdAndTextSearch(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void testSearchMessages_WhitespaceQuery() {
        // Given
        String query = "   ";
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, 0, 10);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalCount());
        
        verify(messageRepository, never()).findByConversationIdAndTextSearch(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void testSearchMessages_FallbackToRegexSearch() {
        // Given
        String query = "hello";
        int page = 0;
        int size = 10;
        
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq(query), any(Pageable.class)))
            .thenThrow(new RuntimeException("Text search failed"));
        when(messageRepository.findByConversationIdAndContentRegex(eq(conversationId), eq("\\Qhello\\E"), any(Pageable.class)))
            .thenReturn(Arrays.asList(message1));
        when(messageRepository.countByConversationIdAndContentRegex(conversationId, "\\Qhello\\E"))
            .thenReturn(1L);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, page, size);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getResultCount());
        assertEquals(1, result.getTotalCount());
        
        verify(messageRepository).findByConversationIdAndTextSearch(eq(conversationId), eq(query), any(Pageable.class));
        verify(messageRepository).findByConversationIdAndContentRegex(eq(conversationId), eq("\\Qhello\\E"), any(Pageable.class));
    }
    
    @Test
    void testSearchMessages_PaginationValidation() {
        // Given
        String query = "hello";
        int page = -1; // Negative page
        int size = 200; // Over max size
        
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq(query), any(Pageable.class)))
            .thenReturn(Arrays.asList(message1));
        when(messageRepository.countByConversationIdAndTextSearch(conversationId, query))
            .thenReturn(1L);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, page, size);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage()); // Page adjusted to 0
        assertEquals(100, result.getPageSize()); // Size capped at max
        
        verify(messageRepository).findByConversationIdAndTextSearch(
            eq(conversationId), 
            eq(query), 
            eq(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "timestamp")))
        );
    }
    
    @Test
    void testSearchMessages_DefaultPagination() {
        // Given
        String query = "hello";
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq(query), any(Pageable.class)))
            .thenReturn(Arrays.asList(message1));
        when(messageRepository.countByConversationIdAndTextSearch(conversationId, query))
            .thenReturn(1L);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        assertEquals(20, result.getPageSize()); // Default page size
        
        verify(messageRepository).findByConversationIdAndTextSearch(
            eq(conversationId), 
            eq(query), 
            eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp")))
        );
    }
    
    @Test
    void testSearchMessages_HasMoreResults() {
        // Given
        String query = "hello";
        int page = 0;
        int size = 1;
        
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq(query), any(Pageable.class)))
            .thenReturn(Arrays.asList(message1));
        when(messageRepository.countByConversationIdAndTextSearch(conversationId, query))
            .thenReturn(2L); // Total is 2, but only 1 returned
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, page, size);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isHasMore());
        assertEquals(1, result.getNextPage());
    }
    
    @Test
    void testGetMessageContext_ValidMessage() {
        // Given
        String messageId = "msg2";
        int contextSize = 3;
        
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message2));
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTimestampBetween(
            eq(conversationId), any(Instant.class), any(Instant.class)))
            .thenReturn(Arrays.asList(message1, message2, message3));
        
        // When
        List<MessageSearchResultDto> result = messageSearchService.getMessageContext(messageId, userId, contextSize);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Should be ordered by timestamp
        assertEquals(message1.getId(), result.get(0).getId());
        assertEquals(message2.getId(), result.get(1).getId());
        assertEquals(message3.getId(), result.get(2).getId());
        
        verify(messageRepository).findById(messageId);
        verify(conversationService).hasUserAccess(userId, conversationId);
    }
    
    @Test
    void testGetMessageContext_MessageNotFound() {
        // Given
        String messageId = "nonexistent";
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        
        // When
        List<MessageSearchResultDto> result = messageSearchService.getMessageContext(messageId, userId, 3);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(messageRepository).findById(messageId);
        verify(conversationService, never()).hasUserAccess(anyString(), anyString());
    }
    
    @Test
    void testGetMessageContext_NoAccess() {
        // Given
        String messageId = "msg2";
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message2));
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(false);
        
        // When
        List<MessageSearchResultDto> result = messageSearchService.getMessageContext(messageId, userId, 3);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(messageRepository).findById(messageId);
        verify(conversationService).hasUserAccess(userId, conversationId);
        verify(messageRepository, never()).findByConversationIdAndTimestampBetween(
            anyString(), any(Instant.class), any(Instant.class));
    }
    
    @Test
    void testHighlightText() {
        // Given
        String text = "Hello world, this is a test message";
        String query = "hello";
        
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq(query), any(Pageable.class)))
            .thenReturn(Arrays.asList(message1));
        when(messageRepository.countByConversationIdAndTextSearch(conversationId, query))
            .thenReturn(1L);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, 0, 10);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        MessageSearchResultDto messageResult = result.getMessages().get(0);
        String highlighted = messageResult.getHighlightedContent();
        
        // Should contain highlighting tags
        assertTrue(highlighted.contains("<mark>"));
        assertTrue(highlighted.contains("</mark>"));
        assertTrue(messageResult.hasHighlighting());
    }
    
    @Test
    void testQuerySanitization() {
        // Given
        String query = "  hello\"world'test\\  ";
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq("helloworldtest"), any(Pageable.class)))
            .thenReturn(Arrays.asList(message1));
        when(messageRepository.countByConversationIdAndTextSearch(conversationId, "helloworldtest"))
            .thenReturn(1L);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, query, userId, 0, 10);
        
        // Then
        assertNotNull(result);
        
        // Verify sanitized query was used
        verify(messageRepository).findByConversationIdAndTextSearch(
            eq(conversationId), 
            eq("helloworldtest"), // Quotes and backslashes removed
            any(Pageable.class)
        );
    }
    
    @Test
    void testLongQueryTruncation() {
        // Given
        String longQuery = "a".repeat(250); // Over 200 character limit
        String expectedQuery = "a".repeat(200); // Should be truncated
        
        when(conversationService.hasUserAccess(userId, conversationId)).thenReturn(true);
        when(messageRepository.findByConversationIdAndTextSearch(eq(conversationId), eq(expectedQuery), any(Pageable.class)))
            .thenReturn(Arrays.asList());
        when(messageRepository.countByConversationIdAndTextSearch(conversationId, expectedQuery))
            .thenReturn(0L);
        
        // When
        SearchResultDto result = messageSearchService.searchMessages(conversationId, longQuery, userId, 0, 10);
        
        // Then
        assertNotNull(result);
        
        // Verify query was truncated
        verify(messageRepository).findByConversationIdAndTextSearch(
            eq(conversationId), 
            eq(expectedQuery), 
            any(Pageable.class)
        );
    }
}
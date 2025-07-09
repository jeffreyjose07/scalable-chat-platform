package com.chatplatform.repository;

import com.chatplatform.model.Conversation;
import com.chatplatform.model.ConversationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for Conversation model without Spring context
 * For repository integration tests, use @DataJpaTest when Maven repository issues are resolved
 */
class ConversationRepositoryTest {
    
    @Test
    public void testConversationCreation() {
        // Given
        String conversationId = "test-conversation";
        String createdBy = "user123";
        
        // When
        Conversation conversation = new Conversation(conversationId, ConversationType.GROUP, "Test Group", createdBy);
        
        // Then
        assertEquals(conversationId, conversation.getId());
        assertEquals(ConversationType.GROUP, conversation.getType());
        assertEquals("Test Group", conversation.getName());
        assertEquals(createdBy, conversation.getCreatedBy());
        assertNotNull(conversation.getCreatedAt());
        assertNotNull(conversation.getUpdatedAt());
        assertTrue(conversation.getParticipants().isEmpty());
    }
    
    @Test
    public void testDirectConversationCreation() {
        // Given
        String conversationId = "dm_user1_user2";
        String createdBy = "user1";
        
        // When
        Conversation conversation = new Conversation(conversationId, ConversationType.DIRECT, null, createdBy);
        
        // Then
        assertEquals(conversationId, conversation.getId());
        assertEquals(ConversationType.DIRECT, conversation.getType());
        assertNull(conversation.getName()); // Direct messages don't have names
        assertEquals(createdBy, conversation.getCreatedBy());
    }
}
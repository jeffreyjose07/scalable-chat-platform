package com.chatplatform.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversationTest {
    
    @Test
    public void testConversationCreation() {
        String conversationId = "test-conversation";
        String createdBy = "user123";
        
        Conversation conversation = new Conversation(conversationId, ConversationType.GROUP, "Test Group", createdBy);
        
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
        String conversationId = "dm_user1_user2";
        String createdBy = "user1";
        
        Conversation conversation = new Conversation(conversationId, ConversationType.DIRECT, null, createdBy);
        
        assertEquals(conversationId, conversation.getId());
        assertEquals(ConversationType.DIRECT, conversation.getType());
        assertNull(conversation.getName()); // Direct messages don't have names
        assertEquals(createdBy, conversation.getCreatedBy());
    }
    
    @Test
    public void testConversationParticipantId() {
        String conversationId = "conv123";
        String userId = "user456";
        
        ConversationParticipantId id = new ConversationParticipantId(conversationId, userId);
        
        assertEquals(conversationId, id.getConversationId());
        assertEquals(userId, id.getUserId());
        
        // Test equality
        ConversationParticipantId id2 = new ConversationParticipantId(conversationId, userId);
        assertEquals(id, id2);
        assertEquals(id.hashCode(), id2.hashCode());
    }
    
    @Test
    public void testConversationParticipantCreation() {
        String conversationId = "conv123";
        String userId = "user456";
        
        ConversationParticipant participant = new ConversationParticipant(conversationId, userId);
        
        assertEquals(conversationId, participant.getConversationId());
        assertEquals(userId, participant.getUserId());
        assertNotNull(participant.getJoinedAt());
        assertTrue(participant.getIsActive());
        assertNull(participant.getLastReadAt());
    }
}
package com.chatplatform.service;

import com.chatplatform.dto.ConversationDto;
import com.chatplatform.model.Conversation;
import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.model.ConversationType;
import com.chatplatform.model.User;
import com.chatplatform.repository.jpa.ConversationRepository;
import com.chatplatform.repository.jpa.ConversationParticipantRepository;
import com.chatplatform.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private ConversationParticipantRepository participantRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ConversationService conversationService;
    
    private User user1;
    private User user2;
    private User user3;
    private Conversation groupConversation;
    private Conversation directConversation;
    
    @BeforeEach
    void setUp() {
        user1 = new User("user1", "john", "john@example.com", "password", "John Doe");
        user2 = new User("user2", "jane", "jane@example.com", "password", "Jane Smith");
        user3 = new User("user3", "bob", "bob@example.com", "password", "Bob Johnson");
        
        groupConversation = new Conversation("group1", ConversationType.GROUP, "Test Group", "user1");
        directConversation = new Conversation("dm_user1_user2", ConversationType.DIRECT, null, "user1");
    }
    
    @Test
    void testCreateDirectConversation_NewConversation() {
        // Given
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        when(conversationRepository.findDirectConversationBetweenUsers("user1", "user2"))
            .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(directConversation);
        when(participantRepository.findByIdConversationIdAndIsActiveTrue("dm_user1_user2"))
            .thenReturn(Arrays.asList(
                new ConversationParticipant("dm_user1_user2", "user1"),
                new ConversationParticipant("dm_user1_user2", "user2")
            ));
        
        // When
        ConversationDto result = conversationService.createDirectConversation("user1", "user2");
        
        // Then
        assertNotNull(result);
        assertEquals("dm_user1_user2", result.getId());
        assertEquals(ConversationType.DIRECT, result.getType());
        assertNull(result.getName());
        assertEquals("user1", result.getCreatedBy());
        assertTrue(result.isDirectMessage());
        
        verify(conversationRepository).save(any(Conversation.class));
        verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
    }
    
    @Test
    void testCreateDirectConversation_ExistingConversation() {
        // Given
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        when(conversationRepository.findDirectConversationBetweenUsers("user1", "user2"))
            .thenReturn(Optional.of(directConversation));
        when(participantRepository.findByIdConversationIdAndIsActiveTrue("dm_user1_user2"))
            .thenReturn(Arrays.asList(
                new ConversationParticipant("dm_user1_user2", "user1"),
                new ConversationParticipant("dm_user1_user2", "user2")
            ));
        
        // When
        ConversationDto result = conversationService.createDirectConversation("user1", "user2");
        
        // Then
        assertNotNull(result);
        assertEquals("dm_user1_user2", result.getId());
        
        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(participantRepository, never()).save(any(ConversationParticipant.class));
    }
    
    @Test
    void testCreateDirectConversation_ConsistentIdOrdering() {
        // Given
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        when(conversationRepository.findDirectConversationBetweenUsers(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(directConversation);
        when(participantRepository.findByIdConversationIdAndIsActiveTrue(anyString()))
            .thenReturn(Arrays.asList());
        
        // When - create with user2 first, then user1
        conversationService.createDirectConversation("user2", "user1");
        
        // Then - should still create dm_user1_user2 (consistent ordering)
        verify(conversationRepository).save(argThat(conversation -> 
            conversation.getId().equals("dm_user1_user2")));
    }
    
    @Test
    void testCreateDirectConversation_UserNotFound() {
        // Given
        when(userRepository.findById("user1")).thenReturn(Optional.empty());
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> conversationService.createDirectConversation("user1", "user2")
        );
        
        assertEquals("User not found: user1", exception.getMessage());
    }
    
    @Test
    void testGetUserConversations() {
        // Given
        when(userRepository.existsById("user1")).thenReturn(true);
        when(conversationRepository.findByParticipantUserId("user1"))
            .thenReturn(Arrays.asList(groupConversation, directConversation));
        when(participantRepository.findByIdConversationIdAndIsActiveTrue("group1"))
            .thenReturn(Arrays.asList(new ConversationParticipant("group1", "user1")));
        when(participantRepository.findByIdConversationIdAndIsActiveTrue("dm_user1_user2"))
            .thenReturn(Arrays.asList(
                new ConversationParticipant("dm_user1_user2", "user1"),
                new ConversationParticipant("dm_user1_user2", "user2")
            ));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("user2")).thenReturn(Optional.of(user2));
        
        // When
        List<ConversationDto> result = conversationService.getUserConversations("user1");
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Check that both group and direct conversations are included
        assertTrue(result.stream().anyMatch(conv -> conv.getType() == ConversationType.GROUP));
        assertTrue(result.stream().anyMatch(conv -> conv.getType() == ConversationType.DIRECT));
    }
    
    @Test
    void testGetUserConversationsByType() {
        // Given
        when(userRepository.existsById("user1")).thenReturn(true);
        when(conversationRepository.findByTypeAndParticipantUserId(ConversationType.GROUP, "user1"))
            .thenReturn(Arrays.asList(groupConversation));
        when(participantRepository.findByIdConversationIdAndIsActiveTrue("group1"))
            .thenReturn(Arrays.asList(new ConversationParticipant("group1", "user1")));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        
        // When
        List<ConversationDto> result = conversationService.getUserConversationsByType("user1", ConversationType.GROUP);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ConversationType.GROUP, result.get(0).getType());
        assertEquals("Test Group", result.get(0).getName());
    }
    
    @Test
    void testHasUserAccess() {
        // Given
        when(participantRepository.existsByIdConversationIdAndIdUserIdAndIsActiveTrue("group1", "user1"))
            .thenReturn(true);
        when(participantRepository.existsByIdConversationIdAndIdUserIdAndIsActiveTrue("group1", "user2"))
            .thenReturn(false);
        
        // When & Then
        assertTrue(conversationService.hasUserAccess("user1", "group1"));
        assertFalse(conversationService.hasUserAccess("user2", "group1"));
    }
    
    @Test
    void testGetConversationForUser_WithAccess() {
        // Given
        when(participantRepository.existsByIdConversationIdAndIdUserIdAndIsActiveTrue("group1", "user1"))
            .thenReturn(true);
        when(conversationRepository.findById("group1")).thenReturn(Optional.of(groupConversation));
        when(participantRepository.findByIdConversationIdAndIsActiveTrue("group1"))
            .thenReturn(Arrays.asList(new ConversationParticipant("group1", "user1")));
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        
        // When
        Optional<ConversationDto> result = conversationService.getConversationForUser("group1", "user1");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("group1", result.get().getId());
    }
    
    @Test
    void testGetConversationForUser_WithoutAccess() {
        // Given
        when(participantRepository.existsByIdConversationIdAndIdUserIdAndIsActiveTrue("group1", "user2"))
            .thenReturn(false);
        
        // When
        Optional<ConversationDto> result = conversationService.getConversationForUser("group1", "user2");
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    void testAddUserToConversation_NewParticipant() {
        // Given
        when(conversationRepository.findById("group1")).thenReturn(Optional.of(groupConversation));
        when(userRepository.existsById("user2")).thenReturn(true);
        when(participantRepository.findByIdConversationIdAndIdUserId("group1", "user2"))
            .thenReturn(Optional.empty());
        
        // When
        conversationService.addUserToConversation("group1", "user2");
        
        // Then
        verify(participantRepository).save(any(ConversationParticipant.class));
    }
    
    @Test
    void testAddUserToConversation_ReactivateUser() {
        // Given
        ConversationParticipant inactiveParticipant = new ConversationParticipant("group1", "user2");
        inactiveParticipant.setIsActive(false);
        
        when(conversationRepository.findById("group1")).thenReturn(Optional.of(groupConversation));
        when(userRepository.existsById("user2")).thenReturn(true);
        when(participantRepository.findByIdConversationIdAndIdUserId("group1", "user2"))
            .thenReturn(Optional.of(inactiveParticipant));
        
        // When
        conversationService.addUserToConversation("group1", "user2");
        
        // Then
        assertTrue(inactiveParticipant.getIsActive());
        verify(participantRepository).save(inactiveParticipant);
    }
    
    @Test
    void testAddUserToConversation_DirectConversation() {
        // Given
        when(conversationRepository.findById("dm_user1_user2")).thenReturn(Optional.of(directConversation));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> conversationService.addUserToConversation("dm_user1_user2", "user3")
        );
        
        assertEquals("Cannot add users to direct conversations", exception.getMessage());
    }
    
    @Test
    void testRemoveUserFromConversation() {
        // Given
        ConversationParticipant participant = new ConversationParticipant("group1", "user1");
        participant.setIsActive(true);
        
        when(participantRepository.findByIdConversationIdAndIdUserId("group1", "user1"))
            .thenReturn(Optional.of(participant));
        
        // When
        conversationService.removeUserFromConversation("group1", "user1");
        
        // Then
        assertFalse(participant.getIsActive());
        verify(participantRepository).save(participant);
    }
    
    @Test
    void testRemoveUserFromConversation_UserNotParticipant() {
        // Given
        when(participantRepository.findByIdConversationIdAndIdUserId("group1", "user2"))
            .thenReturn(Optional.empty());
        
        // When
        conversationService.removeUserFromConversation("group1", "user2");
        
        // Then
        verify(participantRepository, never()).save(any(ConversationParticipant.class));
    }
}
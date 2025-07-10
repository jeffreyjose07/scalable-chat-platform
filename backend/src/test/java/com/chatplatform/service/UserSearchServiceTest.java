package com.chatplatform.service;

import com.chatplatform.dto.UserDto;
import com.chatplatform.model.User;
import com.chatplatform.repository.jpa.UserRepository;
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
class UserSearchServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserSearchService userSearchService;
    
    private User user1;
    private User user2;
    private User user3;
    private User currentUser;
    
    @BeforeEach
    void setUp() {
        currentUser = new User("current", "current", "current@example.com", "password", "Current User");
        user1 = new User("user1", "john", "john@example.com", "password", "John Doe");
        user2 = new User("user2", "jane", "jane@example.com", "password", "Jane Smith");
        user3 = new User("user3", "bob", "bob@example.com", "password", "Bob Johnson");
        
        // Set some realistic data
        user1.setOnline(true);
        user2.setOnline(false);
        user3.setOnline(true);
        
        user1.setLastSeenAt(Instant.now().minusSeconds(3600)); // 1 hour ago
        user2.setLastSeenAt(Instant.now().minusSeconds(7200)); // 2 hours ago
        user3.setLastSeenAt(Instant.now().minusSeconds(1800)); // 30 minutes ago
    }
    
    @Test
    void testSearchUsers_WithValidQuery() {
        // Given
        String query = "john";
        String currentUserId = "current";
        int limit = 10;
        
        List<User> mockUsers = Arrays.asList(user1);
        when(userRepository.findUsersMatchingQuery(eq("john"), eq(currentUserId), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        UserDto userDto = result.get(0);
        assertEquals(user1.getId(), userDto.getId());
        assertEquals(user1.getUsername(), userDto.getUsername());
        assertEquals(user1.getEmail(), userDto.getEmail());
        assertEquals(user1.getDisplayName(), userDto.getDisplayName());
        assertTrue(userDto.isOnline());
        
        verify(userRepository).findUsersMatchingQuery(
            eq("john"), 
            eq(currentUserId), 
            eq(PageRequest.of(0, limit, Sort.by("username").ascending()))
        );
    }
    
    @Test
    void testSearchUsers_WithEmptyQuery() {
        // Given
        String query = "";
        String currentUserId = "current";
        int limit = 10;
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(userRepository, never()).findUsersMatchingQuery(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void testSearchUsers_WithNullQuery() {
        // Given
        String query = null;
        String currentUserId = "current";
        int limit = 10;
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(userRepository, never()).findUsersMatchingQuery(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void testSearchUsers_WithWhitespaceQuery() {
        // Given
        String query = "   ";
        String currentUserId = "current";
        int limit = 10;
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(userRepository, never()).findUsersMatchingQuery(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void testSearchUsers_QuerySanitization() {
        // Given
        String query = "  jOhN@#$%  ";
        String currentUserId = "current";
        int limit = 10;
        
        List<User> mockUsers = Arrays.asList(user1);
        when(userRepository.findUsersMatchingQuery(eq("john@"), eq(currentUserId), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        verify(userRepository).findUsersMatchingQuery(
            eq("john@"), // Special characters removed except allowed ones
            eq(currentUserId), 
            any(Pageable.class)
        );
    }
    
    @Test
    void testSearchUsers_LimitValidation() {
        // Given
        String query = "john";
        String currentUserId = "current";
        int limit = 200; // Over max limit
        
        List<User> mockUsers = Arrays.asList(user1);
        when(userRepository.findUsersMatchingQuery(anyString(), anyString(), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        
        // Verify that the limit was capped at MAX_SEARCH_LIMIT (100)
        verify(userRepository).findUsersMatchingQuery(
            anyString(), 
            anyString(), 
            eq(PageRequest.of(0, 100, Sort.by("username").ascending()))
        );
    }
    
    @Test
    void testSearchUsers_NegativeLimit() {
        // Given
        String query = "john";
        String currentUserId = "current";
        int limit = -5; // Negative limit
        
        List<User> mockUsers = Arrays.asList(user1);
        when(userRepository.findUsersMatchingQuery(anyString(), anyString(), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        
        // Verify that the limit was adjusted to minimum of 1
        verify(userRepository).findUsersMatchingQuery(
            anyString(), 
            anyString(), 
            eq(PageRequest.of(0, 1, Sort.by("username").ascending()))
        );
    }
    
    @Test
    void testSearchUsers_DefaultLimit() {
        // Given
        String query = "john";
        String currentUserId = "current";
        
        List<User> mockUsers = Arrays.asList(user1);
        when(userRepository.findUsersMatchingQuery(anyString(), anyString(), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId);
        
        // Then
        assertNotNull(result);
        
        // Verify that the default limit (20) was used
        verify(userRepository).findUsersMatchingQuery(
            anyString(), 
            anyString(), 
            eq(PageRequest.of(0, 20, Sort.by("username").ascending()))
        );
    }
    
    @Test
    void testSearchUsers_MultipleResults() {
        // Given
        String query = "j";
        String currentUserId = "current";
        int limit = 10;
        
        List<User> mockUsers = Arrays.asList(user1, user2); // Both John and Jane
        when(userRepository.findUsersMatchingQuery(anyString(), anyString(), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.searchUsers(query, currentUserId, limit);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify all users are converted to DTOs
        assertTrue(result.stream().anyMatch(dto -> dto.getUsername().equals("john")));
        assertTrue(result.stream().anyMatch(dto -> dto.getUsername().equals("jane")));
    }
    
    @Test
    void testGetUserSuggestions() {
        // Given
        String currentUserId = "current";
        int limit = 5;
        
        List<User> mockUsers = Arrays.asList(user3, user1, user2); // Sorted by last seen
        when(userRepository.findActiveUsersExcluding(eq(currentUserId), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.getUserSuggestions(currentUserId, limit);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        verify(userRepository).findActiveUsersExcluding(
            eq(currentUserId), 
            eq(PageRequest.of(0, limit, Sort.by("lastSeenAt").descending()))
        );
    }
    
    @Test
    void testGetUserSuggestions_DefaultLimit() {
        // Given
        String currentUserId = "current";
        
        List<User> mockUsers = Arrays.asList(user1, user2);
        when(userRepository.findActiveUsersExcluding(anyString(), any(Pageable.class)))
            .thenReturn(mockUsers);
        
        // When
        List<UserDto> result = userSearchService.getUserSuggestions(currentUserId);
        
        // Then
        assertNotNull(result);
        
        // Verify default limit (20) was used
        verify(userRepository).findActiveUsersExcluding(
            eq(currentUserId), 
            eq(PageRequest.of(0, 20, Sort.by("lastSeenAt").descending()))
        );
    }
    
    @Test
    void testUserExists() {
        // Given
        String userId = "user1";
        when(userRepository.existsById(userId)).thenReturn(true);
        
        // When
        boolean result = userSearchService.userExists(userId);
        
        // Then
        assertTrue(result);
        verify(userRepository).existsById(userId);
    }
    
    @Test
    void testUserExists_NotFound() {
        // Given
        String userId = "nonexistent";
        when(userRepository.existsById(userId)).thenReturn(false);
        
        // When
        boolean result = userSearchService.userExists(userId);
        
        // Then
        assertFalse(result);
        verify(userRepository).existsById(userId);
    }
    
    @Test
    void testGetUserById() {
        // Given
        String userId = "user1";
        when(userRepository.findById(userId)).thenReturn(Optional.of(user1));
        
        // When
        UserDto result = userSearchService.getUserById(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(user1.getId(), result.getId());
        assertEquals(user1.getUsername(), result.getUsername());
        assertEquals(user1.getDisplayName(), result.getDisplayName());
        
        verify(userRepository).findById(userId);
    }
    
    @Test
    void testGetUserById_NotFound() {
        // Given
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When
        UserDto result = userSearchService.getUserById(userId);
        
        // Then
        assertNull(result);
        verify(userRepository).findById(userId);
    }
    
    @Test
    void testUserDtoConversion() {
        // Given - use user1 with all fields set
        user1.setAvatarUrl("https://example.com/avatar.jpg");
        user1.setOnline(true);
        user1.setLastSeenAt(Instant.now());
        
        when(userRepository.findById("user1")).thenReturn(Optional.of(user1));
        
        // When
        UserDto result = userSearchService.getUserById("user1");
        
        // Then
        assertNotNull(result);
        assertEquals(user1.getId(), result.getId());
        assertEquals(user1.getUsername(), result.getUsername());
        assertEquals(user1.getEmail(), result.getEmail());
        assertEquals(user1.getDisplayName(), result.getDisplayName());
        assertEquals(user1.getAvatarUrl(), result.getAvatarUrl());
        assertEquals(user1.isOnline(), result.isOnline());
        assertEquals(user1.getLastSeenAt(), result.getLastSeenAt());
    }
}
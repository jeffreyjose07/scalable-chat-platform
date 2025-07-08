package com.chatplatform.service;

import com.chatplatform.model.User;
import com.chatplatform.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(Instant.now());
        testUser.setOnline(false);
    }
    
    @Test
    void shouldLoadUserByUsername() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // When
        UserDetails userDetails = userService.loadUserByUsername("testuser");
        
        // Then
        assertNotNull(userDetails);
        assertEquals(testUser.getUsername(), userDetails.getUsername());
        assertEquals(testUser.getPassword(), userDetails.getPassword());
        verify(userRepository).findByUsername("testuser");
    }
    
    @Test
    void shouldThrowExceptionWhenUserNotFoundByUsername() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());
        
        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent");
        });
        
        assertEquals("User not found: nonexistent", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }
    
    @Test
    void shouldCreateUserSuccessfully() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        User createdUser = userService.createUser("newuser", "new@example.com", "password123", "New User");
        
        // Then
        assertNotNull(createdUser);
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void shouldThrowExceptionWhenUsernameExists() {
        // Given
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("existinguser", "new@example.com", "password123", "New User");
        });
        
        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByUsername("existinguser");
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }
    
    @Test
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser("newuser", "existing@example.com", "password123", "New User");
        });
        
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("existing@example.com");
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }
    
    @Test
    void shouldFindUserByUsername() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> foundUser = userService.findByUsername("testuser");
        
        // Then
        assertTrue(foundUser.isPresent());
        assertEquals(testUser, foundUser.get());
        verify(userRepository).findByUsername("testuser");
    }
    
    @Test
    void shouldFindUserByEmail() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> foundUser = userService.findByEmail("test@example.com");
        
        // Then
        assertTrue(foundUser.isPresent());
        assertEquals(testUser, foundUser.get());
        verify(userRepository).findByEmail("test@example.com");
    }
    
    @Test
    void shouldFindUserById() {
        // Given
        when(userRepository.findById("1")).thenReturn(Optional.of(testUser));
        
        // When
        Optional<User> foundUser = userService.findById("1");
        
        // Then
        assertTrue(foundUser.isPresent());
        assertEquals(testUser, foundUser.get());
        verify(userRepository).findById("1");
    }
    
    @Test
    void shouldFindAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);
        
        // When
        List<User> foundUsers = userService.findAllUsers();
        
        // Then
        assertEquals(1, foundUsers.size());
        assertEquals(testUser, foundUsers.get(0));
        verify(userRepository).findAll();
    }
    
    @Test
    void shouldUpdateUser() {
        // Given
        when(userRepository.save(testUser)).thenReturn(testUser);
        
        // When
        User updatedUser = userService.updateUser(testUser);
        
        // Then
        assertEquals(testUser, updatedUser);
        verify(userRepository).save(testUser);
    }
    
    @Test
    void shouldUpdateUserOnlineStatus() {
        // Given
        when(userRepository.findById("1")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        userService.updateUserOnlineStatus("1", true);
        
        // Then
        verify(userRepository).findById("1");
        verify(userRepository).save(any(User.class));
        
        // Verify that the user's online status and lastSeenAt were updated
        assertTrue(testUser.isOnline());
        assertNotNull(testUser.getLastSeenAt());
    }
    
    @Test
    void shouldDeleteUser() {
        // When
        userService.deleteUser("1");
        
        // Then
        verify(userRepository).deleteById("1");
    }
    
    @Test
    void shouldValidatePassword() {
        // Given
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        
        // When
        boolean isValid = userService.validatePassword("password123", "hashedPassword");
        
        // Then
        assertTrue(isValid);
        verify(passwordEncoder).matches("password123", "hashedPassword");
    }
    
    @Test
    void shouldRejectInvalidPassword() {
        // Given
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);
        
        // When
        boolean isValid = userService.validatePassword("wrongpassword", "hashedPassword");
        
        // Then
        assertFalse(isValid);
        verify(passwordEncoder).matches("wrongpassword", "hashedPassword");
    }
}
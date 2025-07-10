package com.chatplatform.controller;

import com.chatplatform.dto.UserDto;
import com.chatplatform.dto.UserSearchRequest;
import com.chatplatform.service.UserSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserSearchController {
    
    private final UserSearchService userSearchService;
    
    public UserSearchController(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            Authentication authentication) {
        String currentUserId = authentication.getName();
        List<UserDto> users = userSearchService.searchUsers(query, currentUserId, limit);
        return ResponseEntity.ok(users);
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsersPost(
            @Valid @RequestBody UserSearchRequest request,
            Authentication authentication) {
        String currentUserId = authentication.getName();
        List<UserDto> users = userSearchService.searchUsers(
            request.getQuery(), 
            currentUserId, 
            request.getSize()
        );
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/suggestions")
    public ResponseEntity<List<UserDto>> getUserSuggestions(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            Authentication authentication) {
        String currentUserId = authentication.getName();
        List<UserDto> suggestions = userSearchService.getUserSuggestions(currentUserId, limit);
        return ResponseEntity.ok(suggestions);
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable String userId,
            Authentication authentication) {
        UserDto user = userSearchService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
}
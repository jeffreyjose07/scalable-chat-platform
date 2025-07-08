package com.chatplatform.controller;

import com.chatplatform.model.User;
import com.chatplatform.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        
        // For demo purposes, create a mock user
        User user = new User();
        user.setId("demo-user-" + email.hashCode());
        user.setUsername(email);
        user.setEmail(email);
        user.setDisplayName("Demo User");
        
        // Mock JWT token
        String token = "demo-token-" + System.currentTimeMillis();
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        // Extract token from Authorization header
        String token = authHeader.replace("Bearer ", "");
        
        // For demo purposes, return a mock user based on token
        User user = new User();
        user.setId("demo-user-" + token.hashCode());
        user.setUsername("demo@example.com");
        user.setEmail("demo@example.com");
        user.setDisplayName("Demo User");
        
        return ResponseEntity.ok(user);
    }
}
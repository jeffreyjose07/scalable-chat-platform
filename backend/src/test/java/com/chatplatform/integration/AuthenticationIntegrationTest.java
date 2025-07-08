//package com.chatplatform.integration;
//
//import com.chatplatform.config.TestcontainersConfiguration;
//import com.chatplatform.dto.LoginRequest;
//import com.chatplatform.dto.RegisterRequest;
//import com.chatplatform.model.User;
//import com.chatplatform.repository.jpa.UserRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.hamcrest.Matchers.notNullValue;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureWebMvc
//@ActiveProfiles("test")
//@Import(TestcontainersConfiguration.class)
//@Transactional
//public class AuthenticationIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @BeforeEach
//    void setUp() {
//        // Clean up database before each test
//        userRepository.deleteAll();
//    }
//
//    @Test
//    void shouldRegisterNewUserSuccessfully() throws Exception {
//        // Given
//        RegisterRequest registerRequest = new RegisterRequest();
//        registerRequest.setUsername("newuser");
//        registerRequest.setEmail("newuser@example.com");
//        registerRequest.setPassword("password123");
//        registerRequest.setDisplayName("New User");
//
//        // When & Then
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token", notNullValue()))
//                .andExpect(jsonPath("$.user.username").value("newuser"))
//                .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
//                .andExpect(jsonPath("$.user.displayName").value("New User"));
//
//        // Verify user was saved to database
//        assertTrue(userRepository.existsByUsername("newuser"));
//        assertTrue(userRepository.existsByEmail("newuser@example.com"));
//    }
//
//    @Test
//    void shouldRejectDuplicateUsername() throws Exception {
//        // Given - create a user first
//        RegisterRequest firstUser = new RegisterRequest();
//        firstUser.setUsername("testuser");
//        firstUser.setEmail("first@example.com");
//        firstUser.setPassword("password123");
//        firstUser.setDisplayName("First User");
//
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(firstUser)))
//                .andExpect(status().isOk());
//
//        // When - try to register with same username
//        RegisterRequest duplicateUser = new RegisterRequest();
//        duplicateUser.setUsername("testuser"); // Same username
//        duplicateUser.setEmail("second@example.com");
//        duplicateUser.setPassword("password123");
//        duplicateUser.setDisplayName("Second User");
//
//        // Then
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(duplicateUser)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Username already exists"));
//    }
//
//    @Test
//    void shouldRejectDuplicateEmail() throws Exception {
//        // Given - create a user first
//        RegisterRequest firstUser = new RegisterRequest();
//        firstUser.setUsername("firstuser");
//        firstUser.setEmail("test@example.com");
//        firstUser.setPassword("password123");
//        firstUser.setDisplayName("First User");
//
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(firstUser)))
//                .andExpect(status().isOk());
//
//        // When - try to register with same email
//        RegisterRequest duplicateUser = new RegisterRequest();
//        duplicateUser.setUsername("seconduser");
//        duplicateUser.setEmail("test@example.com"); // Same email
//        duplicateUser.setPassword("password123");
//        duplicateUser.setDisplayName("Second User");
//
//        // Then
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(duplicateUser)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Email already exists"));
//    }
//
//    @Test
//    void shouldLoginWithValidCredentials() throws Exception {
//        // Given - register a user first
//        RegisterRequest registerRequest = new RegisterRequest();
//        registerRequest.setUsername("testuser");
//        registerRequest.setEmail("test@example.com");
//        registerRequest.setPassword("password123");
//        registerRequest.setDisplayName("Test User");
//
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        // When - login with the same credentials
//        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
//
//        // Then
//        mockMvc.perform(post("/api/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token", notNullValue()))
//                .andExpect(jsonPath("$.user.username").value("testuser"))
//                .andExpect(jsonPath("$.user.email").value("test@example.com"));
//    }
//
//    @Test
//    void shouldRejectInvalidCredentials() throws Exception {
//        // Given - register a user first
//        RegisterRequest registerRequest = new RegisterRequest();
//        registerRequest.setUsername("testuser");
//        registerRequest.setEmail("test@example.com");
//        registerRequest.setPassword("password123");
//        registerRequest.setDisplayName("Test User");
//
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        // When - login with wrong password
//        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");
//
//        // Then
//        mockMvc.perform(post("/api/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Invalid email or password"));
//    }
//
//    @Test
//    void shouldRejectLoginForNonexistentUser() throws Exception {
//        // Given
//        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");
//
//        // When & Then
//        mockMvc.perform(post("/api/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Invalid email or password"));
//    }
//
//    @Test
//    void shouldGetCurrentUserWithValidToken() throws Exception {
//        // Given - register and login to get token
//        RegisterRequest registerRequest = new RegisterRequest();
//        registerRequest.setUsername("testuser");
//        registerRequest.setEmail("test@example.com");
//        registerRequest.setPassword("password123");
//        registerRequest.setDisplayName("Test User");
//
//        String registerResponse = mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk())
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        // Extract token from response
//        String token = objectMapper.readTree(registerResponse).get("token").asText();
//
//        // When & Then
//        mockMvc.perform(get("/api/auth/me")
//                .header("Authorization", "Bearer " + token))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.username").value("testuser"))
//                .andExpect(jsonPath("$.email").value("test@example.com"))
//                .andExpect(jsonPath("$.displayName").value("Test User"));
//    }
//
//    @Test
//    void shouldRejectInvalidToken() throws Exception {
//        // When & Then
//        mockMvc.perform(get("/api/auth/me")
//                .header("Authorization", "Bearer invalid-token"))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Invalid or expired token"));
//    }
//
//    @Test
//    void shouldLogoutSuccessfully() throws Exception {
//        // Given - register and login to get token
//        RegisterRequest registerRequest = new RegisterRequest();
//        registerRequest.setUsername("testuser");
//        registerRequest.setEmail("test@example.com");
//        registerRequest.setPassword("password123");
//        registerRequest.setDisplayName("Test User");
//
//        String registerResponse = mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk())
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        // Extract token from response
//        String token = objectMapper.readTree(registerResponse).get("token").asText();
//
//        // When & Then
//        mockMvc.perform(post("/api/auth/logout")
//                .header("Authorization", "Bearer " + token))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("Logout successful"));
//
//        // Verify user is marked as offline
//        User user = userRepository.findByUsername("testuser").orElseThrow();
//        assertFalse(user.isOnline());
//    }
//
//    @Test
//    void shouldValidateRegistrationRequestFields() throws Exception {
//        // Given - invalid registration request
//        RegisterRequest invalidRequest = new RegisterRequest();
//        invalidRequest.setUsername(""); // Invalid - empty
//        invalidRequest.setEmail("invalid-email"); // Invalid format
//        invalidRequest.setPassword("123"); // Invalid - too short
//        invalidRequest.setDisplayName(""); // Invalid - empty
//
//        // When & Then
//        mockMvc.perform(post("/api/auth/register")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(invalidRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void shouldValidateLoginRequestFields() throws Exception {
//        // Given - invalid login request
//        LoginRequest invalidRequest = new LoginRequest("", ""); // Empty fields
//
//        // When & Then
//        mockMvc.perform(post("/api/auth/login")
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(invalidRequest)))
//                .andExpect(status().isBadRequest());
//    }
//}
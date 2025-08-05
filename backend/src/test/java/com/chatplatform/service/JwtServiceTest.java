package com.chatplatform.service;

import com.chatplatform.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    
    private JwtService jwtService;
    private User testUser;
    private final String testSecret = "myTestSecretKey-changeInProduction-veryLongRandomString123456789";
    private final Long testExpiration = 86400000L; // 24 hours
    
    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", testExpiration);
        ReflectionTestUtils.setField(jwtService, "jwtIssuer", "test-issuer");
        ReflectionTestUtils.setField(jwtService, "jwtAudience", "test-audience");
        
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
    }
    
    @Test
    void shouldGenerateValidToken() {
        // When
        String token = jwtService.generateToken(testUser);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT format: header.payload.signature
    }
    
    @Test
    void shouldExtractUsernameFromToken() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        String extractedUsername = jwtService.extractUsername(token);
        
        // Then
        assertEquals(testUser.getUsername(), extractedUsername);
    }
    
    @Test
    void shouldExtractExpirationFromToken() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        Date expiration = jwtService.extractExpiration(token);
        
        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }
    
    @Test
    void shouldValidateTokenWithValidUserDetails() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        Boolean isValid = jwtService.validateToken(token, testUser);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void shouldRejectTokenWithDifferentUser() {
        // Given
        String token = jwtService.generateToken(testUser);
        User differentUser = new User();
        differentUser.setUsername("differentuser");
        
        // When
        Boolean isValid = jwtService.validateToken(token, differentUser);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void shouldValidateTokenWithoutUserDetails() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        Boolean isValid = jwtService.validateToken(token);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";
        
        // When
        Boolean isValid = jwtService.validateToken(invalidToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void shouldRejectExpiredToken() {
        // Given - create JWT service with negative expiration (already expired)
        JwtService shortExpiryService = new JwtService();
        ReflectionTestUtils.setField(shortExpiryService, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(shortExpiryService, "jwtExpiration", -1000L); // Already expired
        
        String token = shortExpiryService.generateToken(testUser);
        
        // When
        Boolean isValid = shortExpiryService.validateToken(token, testUser);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void shouldExtractClaimsCorrectly() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        // When
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        Date issuedAt = jwtService.extractClaim(token, Claims::getIssuedAt);
        
        // Then
        assertEquals(testUser.getUsername(), subject);
        assertNotNull(issuedAt);
        assertTrue(issuedAt.before(new Date()));
    }
}
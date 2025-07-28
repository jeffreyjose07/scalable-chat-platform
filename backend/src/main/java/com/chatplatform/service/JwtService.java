package com.chatplatform.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;
    
    @Value("${app.jwt.issuer}")
    private String jwtIssuer;
    
    @Value("${app.jwt.audience}")
    private String jwtAudience;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public String extractJwtId(String token) {
        return extractClaim(token, Claims::getId);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer(jwtIssuer) // Validate issuer
                .requireAudience(jwtAudience) // Validate audience
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }
    
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return createToken(extraClaims, userDetails.getUsername());
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuer(jwtIssuer)
                .setAudience(jwtAudience)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .setId(java.util.UUID.randomUUID().toString()) // Add unique JWT ID (jti)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate token with blacklist checking
     * This method should be used in security filters
     */
    public boolean validateTokenWithBlacklist(String token, UserDetails userDetails, TokenBlacklistService blacklistService) {
        try {
            // First check basic validation
            if (!validateToken(token, userDetails)) {
                logger.warn("Basic JWT token validation failed for user: {}", userDetails.getUsername());
                return false;
            }
            
            // Then check if token is blacklisted
            try {
                boolean isBlacklisted = blacklistService.isTokenBlacklisted(token);
                if (isBlacklisted) {
                    logger.warn("JWT token is blacklisted for user: {}", userDetails.getUsername());
                }
                return !isBlacklisted;
            } catch (Exception blacklistException) {
                // If blacklist check fails (e.g., Redis down), log warning and allow token
                logger.warn("Blacklist check failed, allowing token: {}", blacklistException.getMessage());
                return true; // Fail open for blacklist issues
            }
        } catch (Exception e) {
            logger.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
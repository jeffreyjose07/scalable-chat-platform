package com.chatplatform.config;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Dynamic CORS configuration that allows requests from local network IPs
 */
public class DynamicCorsConfiguration implements CorsConfigurationSource {

    private static final List<Pattern> ALLOWED_ORIGIN_PATTERNS = Arrays.asList(
        // Private IP ranges for local development
        Pattern.compile("^http://192\\.168\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
        Pattern.compile("^http://10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
        Pattern.compile("^http://172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}:(3000|3001)$"),
        // Localhost patterns for local development
        Pattern.compile("^https?://localhost:(3000|3001)$"),
        Pattern.compile("^https?://127\\.0\\.0\\.1:(3000|3001)$"),
        // Render.com and other production domains
        Pattern.compile("^https://.*\\.onrender\\.com$"),
        Pattern.compile("^https://scalable-chat-platform\\.onrender\\.com$")
    );

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set basic CORS settings
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        // Check if origin matches any allowed pattern
        if (origin != null && isAllowedOrigin(origin)) {
            configuration.setAllowedOrigins(List.of(origin));
        } else {
            // Fallback to allow same-origin requests for single-service deployment
            String host = request.getHeader("Host");
            if (host != null) {
                String sameOrigin = "https://" + host;
                configuration.setAllowedOrigins(Arrays.asList(sameOrigin, "http://localhost:3000", "http://localhost:3001"));
            } else {
                configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:3001"));
            }
        }
        
        return configuration;
    }
    
    private boolean isAllowedOrigin(String origin) {
        return ALLOWED_ORIGIN_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(origin).matches());
    }
}
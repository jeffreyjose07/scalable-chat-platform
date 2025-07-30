package com.chatplatform.config;

import com.chatplatform.security.JwtAuthenticationFilter;
import com.chatplatform.security.RateLimitingFilter;
import com.chatplatform.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RateLimitingFilter rateLimitingFilter, 
                         UserService userService, PasswordEncoder passwordEncoder) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // SECURITY ENHANCEMENT: Add security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny()) // Prevent clickjacking
                .contentTypeOptions(contentTypeOptions -> {}) // Prevent MIME type sniffing
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubDomains(true)
                    .preload(true)
                )
                .addHeaderWriter((request, response) -> {
                    response.setHeader("X-Content-Type-Options", "nosniff");
                    response.setHeader("X-Frame-Options", "DENY");
                    response.setHeader("X-XSS-Protection", "1; mode=block");
                    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                    response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
                    // CSP to prevent XSS attacks
                    response.setHeader("Content-Security-Policy", 
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https:; " +
                        "font-src 'self' data:; " +
                        "connect-src 'self' ws: wss:; " +
                        "frame-ancestors 'none'"
                    );
                })
            )
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws/**").permitAll() // WebSocket endpoint
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/health/**").permitAll() // Health endpoints
                .requestMatchers("/actuator/health").permitAll() // Actuator health check
                .requestMatchers("/health").permitAll() // Health check endpoint
                // Static resources and frontend routes
                .requestMatchers("/", "/login", "/chat/**", "/conversations/**", "/settings/**").permitAll()
                .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/manifest.json").permitAll()
                .requestMatchers("/index.html", "/asset-manifest.json", "/robots.txt").permitAll()
                .requestMatchers("/api/auth/**").authenticated() // Other auth endpoints require auth
                .requestMatchers("/api/messages/**").authenticated() // Message endpoints require auth
                .requestMatchers("/api/**").authenticated() // All other API endpoints require auth
                .anyRequest().permitAll() // Allow everything else (frontend resources)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Use dynamic CORS configuration that allows any local network IP
        return new DynamicCorsConfiguration();
    }
    
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
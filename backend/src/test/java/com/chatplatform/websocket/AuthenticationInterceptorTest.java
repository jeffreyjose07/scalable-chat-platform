package com.chatplatform.websocket;

import com.chatplatform.model.User;
import com.chatplatform.service.JwtService;
import com.chatplatform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    @Mock
    private HttpHeaders headers;

    @InjectMocks
    private AuthenticationInterceptor authenticationInterceptor;

    private User testUser;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User();
        testUser.setId("user1");
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        attributes = new HashMap<>();
    }

    @Test
    void testBeforeHandshake_Success_WithQueryParameter() throws Exception {
        String token = "valid-token";
        String username = "testuser";
        URI uri = new URI("ws://localhost/ws?token=" + token);

        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(request.getURI()).thenReturn(uri);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals(testUser.getId(), attributes.get("userId"));
        assertEquals(testUser.getUsername(), attributes.get("username"));
        assertEquals(token, attributes.get("token"));

        verify(jwtService).validateToken(token);
        verify(jwtService).extractUsername(token);
        verify(userService).findByUsername(username);
    }

    @Test
    void testBeforeHandshake_Success_WithAuthorizationHeader() throws Exception {
        String token = "valid-token";
        String username = "testuser";
        URI uri = new URI("ws://localhost/ws");

        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals(testUser.getId(), attributes.get("userId"));
        assertEquals(testUser.getUsername(), attributes.get("username"));
        assertEquals(token, attributes.get("token"));
    }

    @Test
    void testBeforeHandshake_NoToken() throws Exception {
        URI uri = new URI("ws://localhost/ws");

        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn(null);

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(jwtService, never()).validateToken(any());
        verify(jwtService, never()).extractUsername(any());
        verify(userService, never()).findByUsername(any());
    }

    @Test
    void testBeforeHandshake_InvalidToken() throws Exception {
        String token = "invalid-token";
        URI uri = new URI("ws://localhost/ws?token=" + token);

        when(request.getURI()).thenReturn(uri);
        when(jwtService.validateToken(token)).thenReturn(false);

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(jwtService).validateToken(token);
        verify(jwtService, never()).extractUsername(any());
        verify(userService, never()).findByUsername(any());
    }

    @Test
    void testBeforeHandshake_UserNotFound() throws Exception {
        String token = "valid-token";
        String username = "nonexistent";
        URI uri = new URI("ws://localhost/ws?token=" + token);

        when(request.getURI()).thenReturn(uri);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.empty());

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(jwtService).validateToken(token);
        verify(jwtService).extractUsername(token);
        verify(userService).findByUsername(username);
    }

    @Test
    void testBeforeHandshake_JwtServiceException() throws Exception {
        String token = "valid-token";
        URI uri = new URI("ws://localhost/ws?token=" + token);

        when(request.getURI()).thenReturn(uri);
        when(jwtService.validateToken(token)).thenThrow(new RuntimeException("JWT validation error"));

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(jwtService).validateToken(token);
        verify(jwtService, never()).extractUsername(any());
        verify(userService, never()).findByUsername(any());
    }

    @Test
    void testBeforeHandshake_UserServiceException() throws Exception {
        String token = "valid-token";
        String username = "testuser";
        URI uri = new URI("ws://localhost/ws?token=" + token);

        when(request.getURI()).thenReturn(uri);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenThrow(new RuntimeException("Database error"));

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(jwtService).validateToken(token);
        verify(jwtService).extractUsername(token);
        verify(userService).findByUsername(username);
    }

    @Test
    void testBeforeHandshake_TokenWithMultipleQueryParams() throws Exception {
        String token = "valid-token";
        String username = "testuser";
        URI uri = new URI("ws://localhost/ws?param1=value1&token=" + token + "&param2=value2");

        when(request.getURI()).thenReturn(uri);
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals(testUser.getId(), attributes.get("userId"));
        assertEquals(testUser.getUsername(), attributes.get("username"));
        assertEquals(token, attributes.get("token"));
    }

    @Test
    void testBeforeHandshake_InvalidAuthorizationHeaderFormat() throws Exception {
        URI uri = new URI("ws://localhost/ws");

        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn("InvalidFormat token");

        boolean result = authenticationInterceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertFalse(result);
        assertTrue(attributes.isEmpty());

        verify(jwtService, never()).validateToken(any());
    }

    @Test
    void testAfterHandshake_DoesNothing() {
        // This method is currently empty, so we just verify it doesn't throw exceptions
        assertDoesNotThrow(() -> 
            authenticationInterceptor.afterHandshake(request, response, wsHandler, null)
        );
    }
}
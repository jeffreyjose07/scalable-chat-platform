package com.chatplatform.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't have permission for.
 */
public class AccessDeniedException extends RuntimeException {
    
    private final String resource;
    private final String action;
    private final String userId;
    
    public AccessDeniedException(String message) {
        super(message);
        this.resource = null;
        this.action = null;
        this.userId = null;
    }
    
    public AccessDeniedException(String resource, String action, String userId) {
        super(String.format("Access denied for user %s to %s %s", userId, action, resource));
        this.resource = resource;
        this.action = action;
        this.userId = userId;
    }
    
    public AccessDeniedException(String resource, String action, String userId, String customMessage) {
        super(customMessage);
        this.resource = resource;
        this.action = action;
        this.userId = userId;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getAction() {
        return action;
    }
    
    public String getUserId() {
        return userId;
    }
}
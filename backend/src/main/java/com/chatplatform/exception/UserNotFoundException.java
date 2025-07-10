package com.chatplatform.exception;

/**
 * Exception thrown when a requested user is not found.
 */
public class UserNotFoundException extends RuntimeException {
    
    private final String userId;
    private final String searchCriteria;
    
    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
        this.searchCriteria = "id";
    }
    
    public UserNotFoundException(String searchValue, String searchCriteria) {
        super(String.format("User not found by %s: %s", searchCriteria, searchValue));
        this.userId = searchValue;
        this.searchCriteria = searchCriteria;
    }
    
    public UserNotFoundException(String userId, String searchCriteria, String customMessage) {
        super(customMessage);
        this.userId = userId;
        this.searchCriteria = searchCriteria;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getSearchCriteria() {
        return searchCriteria;
    }
}
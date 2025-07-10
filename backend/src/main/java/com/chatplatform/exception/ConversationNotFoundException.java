package com.chatplatform.exception;

/**
 * Exception thrown when a requested conversation is not found.
 */
public class ConversationNotFoundException extends RuntimeException {
    
    private final String conversationId;
    
    public ConversationNotFoundException(String conversationId) {
        super("Conversation not found: " + conversationId);
        this.conversationId = conversationId;
    }
    
    public ConversationNotFoundException(String conversationId, String message) {
        super(message);
        this.conversationId = conversationId;
    }
    
    public String getConversationId() {
        return conversationId;
    }
}
package com.chatplatform.dto;

import com.chatplatform.model.ChatMessage;

public class MessageDistributionEvent {
    private final ChatMessage message;
    
    public MessageDistributionEvent(ChatMessage message) {
        this.message = message;
    }
    
    public ChatMessage getMessage() {
        return message;
    }
}
package com.chatplatform.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class WebSocketMessage {
    private String type;
    private JsonNode data;
    
    public WebSocketMessage() {}
    
    public WebSocketMessage(String type, JsonNode data) {
        this.type = type;
        this.data = data;
    }
    
    // Getters and setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public JsonNode getData() {
        return data;
    }
    
    public void setData(JsonNode data) {
        this.data = data;
    }
}
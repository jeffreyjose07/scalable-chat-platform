package com.chatplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MessageSearchRequest {
    
    @NotBlank(message = "Search query is required")
    @Size(max = 200, message = "Search query cannot exceed 200 characters")
    private String query;
    
    @Min(value = 0, message = "Page number must be non-negative")
    private int page = 0;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private int size = 20;
    
    // Constructors
    public MessageSearchRequest() {}
    
    public MessageSearchRequest(String query, int page, int size) {
        this.query = query;
        this.page = page;
        this.size = size;
    }
    
    // Getters and setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
}
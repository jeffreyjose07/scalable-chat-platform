package com.chatplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class MessageSearchRequest {
    
    @NotBlank(message = "Search query is required")
    @Size(max = 200, message = "Search query cannot exceed 200 characters")
    private String query;
    
    @Min(value = 0, message = "Page number must be non-negative")
    private int page = 0;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private int size = 20;
    
    // Advanced search filters
    @Size(max = 100, message = "Sender filter cannot exceed 100 characters")
    private String senderUsername;
    
    private Instant dateFrom;
    
    private Instant dateTo;
    
    private Boolean hasMedia;
    
    // Constructors
    public MessageSearchRequest() {}
    
    public MessageSearchRequest(String query, int page, int size) {
        this.query = query;
        this.page = page;
        this.size = size;
    }
    
    public MessageSearchRequest(String query, int page, int size, String senderUsername, 
                              Instant dateFrom, Instant dateTo, Boolean hasMedia) {
        this.query = query;
        this.page = page;
        this.size = size;
        this.senderUsername = senderUsername;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.hasMedia = hasMedia;
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
    
    public String getSenderUsername() {
        return senderUsername;
    }
    
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    
    public Instant getDateFrom() {
        return dateFrom;
    }
    
    public void setDateFrom(Instant dateFrom) {
        this.dateFrom = dateFrom;
    }
    
    public Instant getDateTo() {
        return dateTo;
    }
    
    public void setDateTo(Instant dateTo) {
        this.dateTo = dateTo;
    }
    
    public Boolean getHasMedia() {
        return hasMedia;
    }
    
    public void setHasMedia(Boolean hasMedia) {
        this.hasMedia = hasMedia;
    }
}
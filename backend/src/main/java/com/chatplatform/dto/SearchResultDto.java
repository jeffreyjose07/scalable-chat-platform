package com.chatplatform.dto;

import java.util.List;

public class SearchResultDto {
    private List<MessageSearchResultDto> messages;
    private int totalCount;
    private int currentPage;
    private int pageSize;
    private boolean hasMore;
    private String query;
    private String conversationId;
    
    // Constructors
    public SearchResultDto() {}
    
    public SearchResultDto(List<MessageSearchResultDto> messages, int totalCount, 
                          int currentPage, int pageSize, String query, String conversationId) {
        this.messages = messages;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.query = query;
        this.conversationId = conversationId;
        this.hasMore = (currentPage + 1) * pageSize < totalCount;
    }
    
    // Getters and setters
    public List<MessageSearchResultDto> getMessages() {
        return messages;
    }
    
    public void setMessages(List<MessageSearchResultDto> messages) {
        this.messages = messages;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public boolean isHasMore() {
        return hasMore;
    }
    
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    // Helper methods
    public int getNextPage() {
        return hasMore ? currentPage + 1 : -1;
    }
    
    public int getResultCount() {
        return messages != null ? messages.size() : 0;
    }
    
    public boolean isEmpty() {
        return messages == null || messages.isEmpty();
    }
}
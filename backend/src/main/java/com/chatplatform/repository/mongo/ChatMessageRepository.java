package com.chatplatform.repository.mongo;

import com.chatplatform.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByConversationIdOrderByTimestampAsc(String conversationId);
    List<ChatMessage> findByConversationIdAndTimestampAfterOrderByTimestampAsc(String conversationId, Instant timestamp);
    List<ChatMessage> findBySenderIdOrderByTimestampDesc(String senderId);
    List<ChatMessage> findByTimestampAfterOrderByTimestampAsc(Instant timestamp);
    void deleteByConversationId(String conversationId);
    
    // Text search methods
    @Query("{ 'conversationId': ?0, '$text': { '$search': ?1 } }")
    List<ChatMessage> findByConversationIdAndTextSearch(String conversationId, String searchTerm, Pageable pageable);
    
    // Count total matches for pagination
    @Query(value = "{ 'conversationId': ?0, '$text': { '$search': ?1 } }", count = true)
    long countByConversationIdAndTextSearch(String conversationId, String searchTerm);
    
    // Fallback regex search (case-insensitive)
    @Query("{ 'conversationId': ?0, 'content': { '$regex': ?1, '$options': 'i' } }")
    List<ChatMessage> findByConversationIdAndContentRegex(String conversationId, String regex, Pageable pageable);
    
    // Count for regex search
    @Query(value = "{ 'conversationId': ?0, 'content': { '$regex': ?1, '$options': 'i' } }", count = true)
    long countByConversationIdAndContentRegex(String conversationId, String regex);
    
    // Search by sender in conversation
    @Query("{ 'conversationId': ?0, '$or': [ " +
           "{ 'content': { '$regex': ?1, '$options': 'i' } }, " +
           "{ 'senderUsername': { '$regex': ?1, '$options': 'i' } } " +
           "] }")
    List<ChatMessage> findByConversationIdAndContentOrSender(String conversationId, String searchTerm, Pageable pageable);
    
    // Get messages around a specific message (for context)
    @Query("{ 'conversationId': ?0, 'timestamp': { '$gte': ?1, '$lte': ?2 } }")
    List<ChatMessage> findByConversationIdAndTimestampBetween(String conversationId, Instant startTime, Instant endTime);
}
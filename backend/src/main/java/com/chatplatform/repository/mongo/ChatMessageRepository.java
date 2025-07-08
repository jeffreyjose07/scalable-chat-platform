package com.chatplatform.repository.mongo;

import com.chatplatform.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByConversationIdOrderByTimestampAsc(String conversationId);
    List<ChatMessage> findByConversationIdAndTimestampAfterOrderByTimestampAsc(String conversationId, Instant timestamp);
    List<ChatMessage> findBySenderIdOrderByTimestampDesc(String senderId);
    void deleteByConversationId(String conversationId);
}
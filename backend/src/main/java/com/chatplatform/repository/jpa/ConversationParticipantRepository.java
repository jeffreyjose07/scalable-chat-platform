package com.chatplatform.repository.jpa;

import com.chatplatform.model.ConversationParticipant;
import com.chatplatform.model.ConversationParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipantId> {
    
    // Find participants by conversation ID
    List<ConversationParticipant> findByIdConversationIdAndIsActiveTrue(String conversationId);
    
    // Find all participants by conversation ID (active and inactive)
    List<ConversationParticipant> findByIdConversationId(String conversationId);
    
    // Find participants by user ID
    List<ConversationParticipant> findByIdUserIdAndIsActiveTrue(String userId);
    
    // Check if user is participant in conversation
    boolean existsByIdConversationIdAndIdUserIdAndIsActiveTrue(String conversationId, String userId);
    
    // Find specific participant
    Optional<ConversationParticipant> findByIdConversationIdAndIdUserId(String conversationId, String userId);
    
    // Find specific active participant
    Optional<ConversationParticipant> findByIdConversationIdAndIdUserIdAndIsActiveTrue(String conversationId, String userId);
    
    // Count active participants in conversation
    @Query("SELECT COUNT(p) FROM ConversationParticipant p " +
           "WHERE p.id.conversationId = :conversationId AND p.isActive = true")
    long countActiveParticipants(@Param("conversationId") String conversationId);
    
    // Find conversations where user is participant
    @Query("SELECT p FROM ConversationParticipant p " +
           "JOIN FETCH p.conversation c " +
           "WHERE p.id.userId = :userId AND p.isActive = true " +
           "ORDER BY c.updatedAt DESC")
    List<ConversationParticipant> findUserConversations(@Param("userId") String userId);
    
    // Convenience method alias for migration service
    default List<ConversationParticipant> findByConversationId(String conversationId) {
        return findByIdConversationId(conversationId);
    }
}
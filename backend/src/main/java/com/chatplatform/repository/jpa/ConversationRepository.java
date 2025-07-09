package com.chatplatform.repository.jpa;

import com.chatplatform.model.Conversation;
import com.chatplatform.model.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    // Find conversations by participant user ID
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND p.isActive = true " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findByParticipantUserId(@Param("userId") String userId);
    
    // Find conversations by type and participant user ID
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE c.type = :type AND p.user.id = :userId AND p.isActive = true " +
           "ORDER BY c.updatedAt DESC")
    List<Conversation> findByTypeAndParticipantUserId(@Param("type") ConversationType type, @Param("userId") String userId);
    
    // Find direct conversation between two users
    @Query("SELECT c FROM Conversation c " +
           "WHERE c.type = 'DIRECT' AND c.id IN " +
           "(SELECT p1.conversation.id FROM ConversationParticipant p1 " +
           "WHERE p1.user.id = :userId1 AND p1.isActive = true AND " +
           "p1.conversation.id IN " +
           "(SELECT p2.conversation.id FROM ConversationParticipant p2 " +
           "WHERE p2.user.id = :userId2 AND p2.isActive = true))")
    Optional<Conversation> findDirectConversationBetweenUsers(@Param("userId1") String userId1, @Param("userId2") String userId2);
    
    // Find conversations created by user
    List<Conversation> findByCreatedByOrderByUpdatedAtDesc(String createdBy);
    
    // Find conversations by type
    List<Conversation> findByTypeOrderByUpdatedAtDesc(ConversationType type);
}
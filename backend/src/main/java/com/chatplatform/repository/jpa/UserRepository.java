package com.chatplatform.repository.jpa;

import com.chatplatform.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // User search methods
    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<User> findUsersMatchingQuery(@Param("query") String query, 
                                     @Param("currentUserId") String currentUserId, 
                                     Pageable pageable);
    
    // Get active users excluding current user
    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND u.lastSeenAt IS NOT NULL")
    List<User> findActiveUsersExcluding(@Param("currentUserId") String currentUserId, 
                                       Pageable pageable);
    
    // Get recently active users (last 24 hours)
    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND " +
           "u.lastSeenAt > :since")
    List<User> findRecentlyActiveUsers(@Param("currentUserId") String currentUserId, 
                                      @Param("since") java.time.Instant since, 
                                      Pageable pageable);
    
    // Get online users
    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND u.isOnline = true")
    List<User> findOnlineUsersExcluding(@Param("currentUserId") String currentUserId, 
                                       Pageable pageable);
    
    // Get all users except current user (for group creation, etc.)
    @Query("SELECT u FROM User u WHERE u.id != :currentUserId")
    List<User> findAllExceptCurrentUser(@Param("currentUserId") String currentUserId, 
                                       Pageable pageable);
}
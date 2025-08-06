package com.chatplatform.controller;

import com.chatplatform.service.AdminDatabaseCleanupService;
import com.chatplatform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CRITICAL: Admin-only controller for sensitive operations
 * All endpoints require admin authentication and are heavily logged
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private AdminDatabaseCleanupService cleanupService;
    
    @Autowired
    private UserService userService;
    
    /**
     * CRITICAL: Preview database cleanup without making changes
     * Shows what would be deleted in a real cleanup operation
     */
    @GetMapping("/cleanup/preview")
    public ResponseEntity<Map<String, Object>> getCleanupPreview(Authentication authentication) {
        String userId = getUserId(authentication);
        logger.warn("ADMIN CLEANUP PREVIEW requested by user: {}", userId);
        
        try {
            // Verify admin permissions
            if (!isAdminUser(userId)) {
                logger.error("UNAUTHORIZED admin preview attempt by user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
            }
            
            // Get cleanup preview (dry run)
            Map<String, Object> preview = cleanupService.getCleanupPreview();
            preview.put("requestedBy", userId);
            preview.put("operation", "PREVIEW_ONLY");
            
            logger.info("Admin cleanup preview completed for user: {}", userId);
            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            logger.error("Error generating cleanup preview for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate preview: " + e.getMessage()));
        }
    }
    
    /**
     * CRITICAL: Execute comprehensive database cleanup
     * This PERMANENTLY DELETES data - requires explicit confirmation
     */
    @PostMapping("/cleanup/execute")
    public ResponseEntity<Map<String, Object>> executeCleanup(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        String userId = getUserId(authentication);
        logger.error("CRITICAL: ADMIN CLEANUP EXECUTION requested by user: {}", userId);
        
        try {
            // Verify admin permissions
            if (!isAdminUser(userId)) {
                logger.error("UNAUTHORIZED admin cleanup attempt by user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
            }
            
            // Require explicit confirmation
            Boolean confirmed = (Boolean) request.get("confirmed");
            String confirmationText = (String) request.get("confirmationText");
            
            if (!Boolean.TRUE.equals(confirmed) || !"DELETE_ORPHANED_DATA".equals(confirmationText)) {
                logger.warn("Admin cleanup attempted without proper confirmation by user: {}", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Explicit confirmation required"));
            }
            
            // Log the critical operation
            logger.error("EXECUTING DATABASE CLEANUP - Admin: {} - PERMANENT DATA DELETION IN PROGRESS", userId);
            
            // Execute cleanup (not dry run)
            Map<String, Object> result = cleanupService.performComprehensiveCleanup(false);
            result.put("executedBy", userId);
            result.put("operation", "CLEANUP_EXECUTED");
            
            logger.error("DATABASE CLEANUP COMPLETED - Admin: {} - Result: {}", userId, result);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("CRITICAL ERROR during database cleanup by admin: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Cleanup failed: " + e.getMessage(), "executedBy", userId));
        }
    }
    
    /**
     * Get database statistics for admin dashboard
     */
    @GetMapping("/stats/database")
    public ResponseEntity<Map<String, Object>> getDatabaseStats(Authentication authentication) {
        String userId = getUserId(authentication);
        logger.info("Database stats requested by user: {}", userId);
        
        try {
            // Verify admin permissions
            if (!isAdminUser(userId)) {
                logger.warn("Unauthorized database stats request by user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
            }
            
            // Generate basic stats without cleanup analysis
            Map<String, Object> stats = new HashMap<>();
            stats.put("requestedBy", userId);
            stats.put("timestamp", java.time.LocalDateTime.now());
            
            // This would be expanded with actual database stats
            stats.put("note", "Basic database statistics - use cleanup preview for detailed analysis");
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting database stats for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get stats: " + e.getMessage()));
        }
    }
    
    /**
     * Admin endpoint to check current user's admin status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAdminStatus(Authentication authentication) {
        String userId = getUserId(authentication);
        boolean isAdmin = isAdminUser(userId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("isAdmin", isAdmin);
        status.put("timestamp", java.time.LocalDateTime.now());
        
        if (isAdmin) {
            logger.info("Admin status check - confirmed admin: {}", userId);
        } else {
            logger.debug("Admin status check - non-admin user: {}", userId);
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Extract user ID from authentication
     */
    private String getUserId(Authentication authentication) {
        return authentication.getName();
    }
    
    /**
     * Check if user has admin privileges
     * CRITICAL: This determines access to dangerous operations
     */
    private boolean isAdminUser(String userId) {
        try {
            // Check if user is admin in the database
            var userOptional = userService.findByUsername(userId);
            boolean isAdmin = userOptional.isPresent() && "admin".equalsIgnoreCase(userOptional.get().getUsername());
            
            logger.debug("Admin check for user {}: {}", userId, isAdmin);
            return isAdmin;
            
        } catch (Exception e) {
            logger.error("Error checking admin status for user: {}", userId, e);
            return false; // Fail closed - deny access on error
        }
    }
}
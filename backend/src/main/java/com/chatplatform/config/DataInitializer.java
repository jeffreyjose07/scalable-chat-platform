package com.chatplatform.config;

import com.chatplatform.model.User;
import com.chatplatform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    private final UserService userService;
    
    @Value("${app.admin.username:admin}")
    private String adminUsername;
    
    @Value("${app.admin.email:admin@chatplatform.com}")
    private String adminEmail;
    
    @Value("${app.admin.password:admin123}")
    private String adminPassword;
    
    @Value("${app.admin.displayName:System Administrator}")
    private String adminDisplayName;
    
    public DataInitializer(@Lazy UserService userService) {
        this.userService = userService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
        createSampleUsers();
    }
    
    private void initializeAdminUser() {
        try {
            // Check if admin user already exists
            if (userService.findByUsername(adminUsername).isEmpty() && 
                userService.findByEmail(adminEmail).isEmpty()) {
                
                User adminUser = userService.createUser(
                    adminUsername,
                    adminEmail,
                    adminPassword,
                    adminDisplayName
                );
                
                logger.info("Created admin user: {} with email: {}", adminUsername, adminEmail);
                logger.warn("SECURITY: Please change the default admin password in production!");
            } else {
                logger.info("Admin user already exists, skipping creation");
            }
        } catch (Exception e) {
            logger.error("Failed to create admin user", e);
        }
    }
    
    private void createSampleUsers() {
        String[][] sampleUsers = {
            {"alice", "alice@example.com", "password123", "Alice Johnson"},
            {"bob", "bob@example.com", "password123", "Bob Smith"},
            {"charlie", "charlie@example.com", "password123", "Charlie Brown"}
        };
        
        for (String[] userData : sampleUsers) {
            try {
                if (userService.findByUsername(userData[0]).isEmpty() && 
                    userService.findByEmail(userData[1]).isEmpty()) {
                    
                    userService.createUser(userData[0], userData[1], userData[2], userData[3]);
                    logger.info("Created sample user: {}", userData[0]);
                }
            } catch (Exception e) {
                logger.debug("Sample user {} already exists or creation failed", userData[0]);
            }
        }
    }
}
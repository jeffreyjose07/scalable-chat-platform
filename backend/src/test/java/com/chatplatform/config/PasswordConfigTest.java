package com.chatplatform.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PasswordConfigTest {

    @Test
    void testPasswordEncoderBean() {
        ApplicationContext context = new AnnotationConfigApplicationContext(PasswordConfig.class);
        PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
        
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
        
        // Test that it can encode and verify passwords
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("wrongPassword", encodedPassword));
    }

    @Test
    void testPasswordEncoderConsistency() {
        PasswordConfig config = new PasswordConfig();
        PasswordEncoder encoder1 = config.passwordEncoder();
        PasswordEncoder encoder2 = config.passwordEncoder();
        
        // Both should be BCryptPasswordEncoder instances
        assertNotNull(encoder1);
        assertNotNull(encoder2);
        assertTrue(encoder1 instanceof BCryptPasswordEncoder);
        assertTrue(encoder2 instanceof BCryptPasswordEncoder);
        
        // They should encode the same password differently (due to salt)
        String password = "testPassword";
        String encoded1 = encoder1.encode(password);
        String encoded2 = encoder2.encode(password);
        
        assertNotEquals(encoded1, encoded2);
        assertTrue(encoder1.matches(password, encoded1));
        assertTrue(encoder2.matches(password, encoded2));
    }
}
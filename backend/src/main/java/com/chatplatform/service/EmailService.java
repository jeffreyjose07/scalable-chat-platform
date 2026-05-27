package com.chatplatform.service;

public interface EmailService {
    /**
     * Send password reset email to user
     * 
     * @param to Recipient email address
     * @param resetToken Password reset token
     * @param userName User's display name
     * @throws Exception if email sending fails
     */
    void sendPasswordResetEmail(String to, String resetToken, String userName) throws Exception;
}

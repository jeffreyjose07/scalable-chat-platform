package com.chatplatform.service.impl;

import com.chatplatform.service.EmailService;
import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResendEmailService.class);
    
    private final Resend resend;
    private final String fromEmail;
    private final String frontendUrl;
    
    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email:onboarding@resend.dev}") String fromEmail,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
        this.frontendUrl = frontendUrl;
        logger.info("ResendEmailService initialized with from: {}", fromEmail);
    }
    
    @Override
    public void sendPasswordResetEmail(String to, String resetToken, String userName) throws Exception {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            
            String htmlContent = buildPasswordResetEmailHtml(userName, resetLink);
            String textContent = buildPasswordResetEmailText(userName, resetLink);
            
            SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject("Reset Your Password - Chat Platform")
                    .html(htmlContent)
                    .text(textContent)
                    .build();
            
            SendEmailResponse response = resend.emails().send(sendEmailRequest);
            logger.info("Password reset email sent successfully. Email ID: {}", response.getId());
            
        } catch (ResendException e) {
            logger.error("Failed to send password reset email to {}: {}", to, e.getMessage(), e);
            throw new Exception("Failed to send password reset email", e);
        }
    }
    
    private String buildPasswordResetEmailHtml(String userName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset Your Password</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f7;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; max-width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 40px; text-align: center; border-radius: 8px 8px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">Chat Platform</h1>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 40px 30px 40px;">
                                        <h2 style="margin: 0 0 20px 0; color: #1f2937; font-size: 24px; font-weight: 600;">Reset Your Password</h2>
                                        <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                                            Hi %s,
                                        </p>
                                        <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                                            We received a request to reset your password. Click the button below to create a new password:
                                        </p>
                                        <table role="presentation" style="margin: 30px 0; width: 100%%;">
                                            <tr>
                                                <td align="center">
                                                    <a href="%s" style="display: inline-block; padding: 14px 32px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: #ffffff; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.25);">Reset Password</a>
                                                </td>
                                            </tr>
                                        </table>
                                        <p style="margin: 0 0 20px 0; color: #6b7280; font-size: 14px; line-height: 1.6;">
                                            This link will expire in <strong>30 minutes</strong> for security reasons.
                                        </p>
                                        <p style="margin: 0 0 20px 0; color: #6b7280; font-size: 14px; line-height: 1.6;">
                                            If you didn't request a password reset, you can safely ignore this email. Your password will not be changed.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f9fafb; border-radius: 0 0 8px 8px; border-top: 1px solid #e5e7eb;">
                                        <p style="margin: 0 0 10px 0; color: #9ca3af; font-size: 12px; line-height: 1.5; text-align: center;">
                                            If the button doesn't work, copy and paste this link into your browser:
                                        </p>
                                        <p style="margin: 0; color: #667eea; font-size: 12px; line-height: 1.5; text-align: center; word-break: break-all;">
                                            <a href="%s" style="color: #667eea;">%s</a>
                                        </p>
                                        <p style="margin: 20px 0 0 0; color: #9ca3af; font-size: 12px; line-height: 1.5; text-align: center;">
                                            © 2025 Chat Platform. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(userName, resetLink, resetLink, resetLink);
    }
    
    private String buildPasswordResetEmailText(String userName, String resetLink) {
        return """
            Reset Your Password
            
            Hi %s,
            
            We received a request to reset your password for your Chat Platform account.
            
            To reset your password, click the link below or copy and paste it into your browser:
            
            %s
            
            This link will expire in 30 minutes for security reasons.
            
            If you didn't request a password reset, you can safely ignore this email. Your password will not be changed.
            
            Best regards,
            The Chat Platform Team
            
            © 2025 Chat Platform. All rights reserved.
            """.formatted(userName, resetLink);
    }
}

package com.smartinventory.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name}")
    private String appName;

    @Async
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject(appName + " - Email Verification OTP");

            String html = buildOtpEmailHtml(otp, purpose);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            // Don't rethrow - OTP is still saved to DB, user can use /otp-debug in dev
        }
    }

    private String buildOtpEmailHtml(String otp, String purpose) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 10px;
                                 box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden; }
                    .header { background: linear-gradient(135deg, #1976d2, #42a5f5); color: white;
                              padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .body { padding: 40px 30px; color: #333; }
                    .otp-box { background: #f0f7ff; border: 2px dashed #1976d2; border-radius: 8px;
                               text-align: center; padding: 20px; margin: 25px 0; }
                    .otp-code { font-size: 42px; font-weight: bold; color: #1976d2;
                                letter-spacing: 10px; font-family: monospace; }
                    .expiry { color: #666; font-size: 14px; margin-top: 10px; }
                    .footer { background: #f9f9f9; padding: 20px 30px; text-align: center;
                              font-size: 12px; color: #999; }
                    .warning { background: #fff3e0; border-left: 4px solid #ff9800;
                               padding: 12px 16px; margin-top: 20px; border-radius: 4px;
                               font-size: 13px; color: #e65100; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <h1>🏪 Smart Inventory Platform</h1>
                      <p style="margin:8px 0 0">Email Verification</p>
                    </div>
                    <div class="body">
                      <p>Hello,</p>
                      <p>You requested an OTP for <strong>%s</strong>. Use the code below to proceed:</p>
                      <div class="otp-box">
                        <div class="otp-code">%s</div>
                        <div class="expiry">⏱ This OTP expires in <strong>5 minutes</strong></div>
                      </div>
                      <div class="warning">
                        🔒 Never share this OTP with anyone. Our team will never ask for it.
                      </div>
                      <p style="margin-top:25px; color:#666; font-size:13px;">
                        If you didn't request this, please ignore this email.
                      </p>
                    </div>
                    <div class="footer">
                      © 2025 Smart Inventory Platform. All rights reserved.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(purpose, otp);
    }
}

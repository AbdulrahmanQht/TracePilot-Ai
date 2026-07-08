package com.tracepilot.api.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${tracepilot.backend.url}")
    private String backendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Verify your email");
        message.setText("""
                Click the link below to verify your email:

                %s/api/v1/auth/verify-email?token=%s

                This link expires in 24 hours.
                """.formatted(backendUrl, token));

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Reset your password");
        message.setText("""
                Click the link below to reset your password:

                %s/api/v1/auth/reset-password?token=%s

                This link expires in 1 hour.
                """.formatted(backendUrl, token));

        mailSender.send(message);
    }
}
package com.tracepilot.api.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import lombok.extern.slf4j.Slf4j;

@Service
public class EmailService {

    private final Resend resend;

    @Value("${tracepilot.backend.url}")
    private String backendUrl;

    @Value("${tracepilot.frontend.url}")
    private String frontendUrl;

    public EmailService(Resend resend) {
        this.resend = resend;
    }

    @Async
    public void sendVerificationEmail(String email, String token) {
        String verifyUrl = backendUrl + "/api/v1/auth/verify-email?token=" + token;

        send(
                email,
                "Verify your email",
                """
                        <p>Click the link below to verify your email:</p>
                        <p><a href="%s">Verify Email</a></p>
                        <p>This link expires in 24 hours.</p>
                        """.formatted(verifyUrl));
    }

    @Async
    public void sendPasswordResetEmail(String email, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        send(
                email,
                "Reset your password",
                """
                        <p>Click the link below to reset your password:</p>
                        <p><a href="%s">Reset Password</a></p>
                        <p>This link expires in 1 hour.</p>
                        """.formatted(resetUrl));
    }
    
    private void send(String to, String subject, String html) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("TracePilot <noreply@trace-pilot.dev>")
                .to(to)
                .subject(subject)
                .html(html)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("Email sent: " + response.getId());
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }
}
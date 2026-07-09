package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "backendUrl", "https://api.tracepilot.test");
    }

    @Test
    void sendVerificationEmail_sendsMessageWithCorrectRecipientAndLink() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendVerificationEmail("abdulrahman@example.com", "verify-token-123");

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getTo()).containsExactly("abdulrahman@example.com");
        assertThat(message.getSubject()).isEqualTo("Verify your email");
        assertThat(message.getText())
                .contains("https://api.tracepilot.test/api/v1/auth/verify-email?token=verify-token-123");
    }

    @Test
    void sendPasswordResetEmail_sendsMessageWithCorrectRecipientAndLink() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendPasswordResetEmail("abdulrahman@example.com", "reset-token-456");

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getTo()).containsExactly("abdulrahman@example.com");
        assertThat(message.getSubject()).isEqualTo("Reset your password");
        assertThat(message.getText())
                .contains("https://api.tracepilot.test/api/v1/auth/reset-password?token=reset-token-456");
    }

    @Test
    void sendVerificationEmail_propagatesMailExceptionsToTheCaller() {
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP unreachable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> emailService.sendVerificationEmail("a@b.com", "token"))
                .isInstanceOf(org.springframework.mail.MailException.class);
    }
}

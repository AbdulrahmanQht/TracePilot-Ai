package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

        @Mock
        private Resend resend;

        @Mock
        private Emails emails;

        private EmailService emailService;

        @BeforeEach
        void setUp() {
                when(resend.emails()).thenReturn(emails);

                emailService = new EmailService(resend);

                ReflectionTestUtils.setField(
                                emailService,
                                "backendUrl",
                                "https://api.tracepilot.test");

                ReflectionTestUtils.setField(
                                emailService,
                                "frontendUrl",
                                "https://tracepilot.test");
        }

        @Test
        void sendVerificationEmail_sendsMessageWithCorrectRecipientAndLink() throws Exception {

                when(emails.send(any(CreateEmailOptions.class)))
                                .thenReturn(new CreateEmailResponse("email-id"));

                emailService.sendVerificationEmail(
                                "abdulrahman@example.com",
                                "verify-token-123");

                ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);

                verify(emails).send(captor.capture());

                CreateEmailOptions options = captor.getValue();

                assertThat(options.getTo()).containsExactly("abdulrahman@example.com");
                assertThat(options.getSubject()).isEqualTo("Verify your email");
                assertThat(options.getHtml())
                                .contains("https://api.tracepilot.test/api/v1/auth/verify-email?token=verify-token-123");
        }

        @Test
        void sendPasswordResetEmail_sendsMessageWithCorrectRecipientAndLink() throws Exception {

                when(emails.send(any(CreateEmailOptions.class)))
                                .thenReturn(new CreateEmailResponse("email-id"));

                emailService.sendPasswordResetEmail(
                                "abdulrahman@example.com",
                                "reset-token-456");

                ArgumentCaptor<CreateEmailOptions> captor = ArgumentCaptor.forClass(CreateEmailOptions.class);

                verify(emails).send(captor.capture());

                CreateEmailOptions options = captor.getValue();

                assertThat(options.getTo()).containsExactly("abdulrahman@example.com");
                assertThat(options.getSubject()).isEqualTo("Reset your password");
                assertThat(options.getHtml())
                                .contains("https://tracepilot.test/reset-password?token=reset-token-456");
        }

}

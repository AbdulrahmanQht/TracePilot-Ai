package com.tracepilot.api.Messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Repositories.TraceAuditRepository;

@ExtendWith(MockitoExtension.class)
class AuditJobDeadLetterListenerTest {

    @Mock
    private TraceAuditRepository auditRepository;

    private AuditJobDeadLetterListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuditJobDeadLetterListener(auditRepository, new ObjectMapper());
    }

    private Message messageWithBody(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    @Test
    void handleDeadLetteredJob_marksAuditFailed_whenAuditExists() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = new TraceAudit();
        audit.setId(auditId);
        audit.setStatus(AuditStatus.PENDING);

        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        listener.handleDeadLetteredJob(messageWithBody("{\"auditId\": \"" + auditId + "\"}"));

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.FAILED);
        verify(auditRepository).save(audit);
    }

    @Test
    void handleDeadLetteredJob_doesNothing_whenAuditIdMissingFromPayload() {
        listener.handleDeadLetteredJob(messageWithBody("{\"foo\": \"bar\"}"));

        verify(auditRepository, never()).findById(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void handleDeadLetteredJob_doesNothing_whenAuditIdIsNullInPayload() {
        listener.handleDeadLetteredJob(messageWithBody("{\"auditId\": null}"));

        verify(auditRepository, never()).findById(any());
    }

    @Test
    void handleDeadLetteredJob_doesNotThrow_whenPayloadIsMalformedJson() {
        org.assertj.core.api.Assertions.assertThatCode(
                () -> listener.handleDeadLetteredJob(messageWithBody("not-json-at-all")))
                .doesNotThrowAnyException();

        verify(auditRepository, never()).save(any());
    }

    @Test
    void handleDeadLetteredJob_doesNotThrow_whenAuditIdReferencesUnknownAudit() {
        UUID auditId = UUID.randomUUID();
        when(auditRepository.findById(auditId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatCode(
                () -> listener.handleDeadLetteredJob(messageWithBody("{\"auditId\": \"" + auditId + "\"}")))
                .doesNotThrowAnyException();

        verify(auditRepository, never()).save(any());
    }

    @Test
    void handleDeadLetteredJob_doesNotThrow_whenAuditIdIsNotAValidUuid() {
        org.assertj.core.api.Assertions.assertThatCode(
                () -> listener.handleDeadLetteredJob(messageWithBody("{\"auditId\": \"not-a-uuid\"}")))
                .doesNotThrowAnyException();

        verify(auditRepository, never()).save(any());
    }
}

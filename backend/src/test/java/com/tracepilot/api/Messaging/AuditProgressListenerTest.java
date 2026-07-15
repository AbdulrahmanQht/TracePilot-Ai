package com.tracepilot.api.Messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tracepilot.api.DTO.Messages.AuditProgressMessage;
import com.tracepilot.api.Enums.AuditProgressStatus;
import com.tracepilot.api.Enums.TypesOfAgent;
import com.tracepilot.api.Services.AuditEmitterRegistry;

@ExtendWith(MockitoExtension.class)
class AuditProgressListenerTest {

    @Mock
    private AuditEmitterRegistry auditEmitterRegistry;

    private AuditProgressListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuditProgressListener(auditEmitterRegistry);
    }

    @Test
    void handleProgress_forwardsMessage_toEmitterRegistry() {
        UUID auditId = UUID.randomUUID();
        AuditProgressMessage message = new AuditProgressMessage(auditId, TypesOfAgent.TRACE_LOOP_EFFICIENCY,
                "loop", AuditProgressStatus.DONE, "Loop efficiency agent finished");

        listener.handleProgress(message);

        verify(auditEmitterRegistry).push(auditId, message);
    }

    @Test
    void handleProgress_forwardsStartedEvent_toEmitterRegistry() {
        UUID auditId = UUID.randomUUID();
        AuditProgressMessage message = new AuditProgressMessage(auditId, TypesOfAgent.BLIND_OUTCOME_VERIFIER,
                "blind_outcome", AuditProgressStatus.STARTED, null);

        listener.handleProgress(message);

        verify(auditEmitterRegistry).push(auditId, message);
    }

    @Test
    void handleProgress_doesNothing_whenMessageIsNull() {
        listener.handleProgress(null);

        verify(auditEmitterRegistry, never()).push(any(), any());
    }

    @Test
    void handleProgress_doesNothing_whenAuditIdIsNull() {
        AuditProgressMessage message = new AuditProgressMessage(null, TypesOfAgent.RELIABILITY_TREND,
                "reliability", AuditProgressStatus.DONE, "unreachable audit id");

        listener.handleProgress(message);

        verify(auditEmitterRegistry, never()).push(any(), any());
    }
}
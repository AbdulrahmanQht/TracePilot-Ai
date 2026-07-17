package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Enums.AuditStatus;

class AuditEmitterRegistryTest {

    private AuditEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AuditEmitterRegistry();
    }

    private AuditResponse sampleResponse(UUID id, AuditStatus status) {
        return new AuditResponse(
                id,
                "Title",
                "tracepilot",
                "GENERIC",
                AuditInputSource.PASTED_TEXT,
                status,
                null,
                false,
                null, // shareToken
                null, // failureReason
                Instant.now(),
                null,
                List.of());
    }

    @Test
    void register_returnsNonNullEmitter() {
        UUID auditId = UUID.randomUUID();

        SseEmitter emitter = registry.register(auditId);

        assertThat(emitter).isNotNull();
    }

    @Test
    void pushAndComplete_doesNothing_whenNoEmitterRegisteredForAudit() {
        UUID auditId = UUID.randomUUID();

        org.assertj.core.api.Assertions.assertThatCode(
                () -> registry.pushAndComplete(auditId, sampleResponse(auditId, AuditStatus.COMPLETE)))
                .doesNotThrowAnyException();
    }

    @Test
    void push_doesNothing_whenNoEmitterRegisteredForAudit() {
        UUID auditId = UUID.randomUUID();

        org.assertj.core.api.Assertions.assertThatCode(
                () -> registry.push(auditId, "some progress payload"))
                .doesNotThrowAnyException();
    }

    @Test
    void pushAndComplete_sendsStatusEvent_toRegisteredEmitter() {
        UUID auditId = UUID.randomUUID();
        SseEmitter emitter = registry.register(auditId);

        registry.pushAndComplete(auditId, sampleResponse(auditId, AuditStatus.COMPLETE));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> emitter.send("Test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void pushAndComplete_removesEmitter_soSecondPushIsNoOp() {
        UUID auditId = UUID.randomUUID();
        registry.register(auditId);

        registry.pushAndComplete(auditId, sampleResponse(auditId, AuditStatus.COMPLETE));

        org.assertj.core.api.Assertions.assertThatCode(
                () -> registry.pushAndComplete(auditId, sampleResponse(auditId, AuditStatus.COMPLETE)))
                .doesNotThrowAnyException();
    }

    @Test
    void push_doesNotCompleteEmitter_soFollowUpPushAndCompleteStillWorks() throws Exception {
        UUID auditId = UUID.randomUUID();
        SseEmitter emitter = registry.register(auditId);

        registry.push(auditId, "agent started");

        org.assertj.core.api.Assertions.assertThatCode(() -> emitter.send("Ping"))
                .doesNotThrowAnyException();

        registry.pushAndComplete(auditId, sampleResponse(auditId, AuditStatus.COMPLETE));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> emitter.send("Test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
    }
}
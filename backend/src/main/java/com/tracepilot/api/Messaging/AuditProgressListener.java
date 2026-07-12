package com.tracepilot.api.Messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.tracepilot.api.DTO.Messages.AuditProgressMessage;
import com.tracepilot.api.Services.AuditEmitterRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditProgressListener {
    private final AuditEmitterRegistry auditEmitterRegistry;

    public AuditProgressListener(AuditEmitterRegistry auditEmitterRegistry) {
        this.auditEmitterRegistry = auditEmitterRegistry;
    }

    @RabbitListener(queues = "audit.progress")
    public void handleProgress(AuditProgressMessage message) {
        if (message == null) {
            log.warn("Received null message from audit.progress queue");
            return;
        }

        if (message.auditId() == null) {
            log.warn("Received audit.progress message with null auditId: {}", message);
            return;
        }

        log.debug(
                "Received audit progress event. auditId={}, agent={}, status={}, step={}",
                message.auditId(),
                message.agentType(),
                message.status(),
                message.step());

        auditEmitterRegistry.push(message.auditId(), message);

        log.trace("Forwarded audit progress event to SSE clients for audit {}", message.auditId());
    }
}

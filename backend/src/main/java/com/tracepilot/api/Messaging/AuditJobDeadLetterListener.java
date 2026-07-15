package com.tracepilot.api.Messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Repositories.TraceAuditRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditJobDeadLetterListener {

    private final TraceAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AuditJobDeadLetterListener(TraceAuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "audit.jobs.dlq")
    @Transactional
    public void handleDeadLetteredJob(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            UUID auditId = tryExtractAuditId(body);

            if (auditId == null) {
                log.error("Dead-lettered job could not be attributed to any audit. Raw body: {}", body);
                return;
            }

            auditRepository.findById(auditId).ifPresentOrElse(audit -> {
                audit.setStatus(AuditStatus.FAILED);
                audit.setFailureReason("Job failed after repeated processing attempts and was dead-lettered.");
                auditRepository.save(audit);
                log.error("Audit {} marked FAILED after landing in the dead-letter queue.", auditId);
            }, () -> log.error(
                    "Dead-lettered job referenced auditId={} but no matching TraceAudit row exists.", auditId));
        } catch (Exception e) {
            log.error("Unexpected failure processing dead-lettered job, dropping. Raw body: {}", body, e);
        }
    }

    private UUID tryExtractAuditId(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode idNode = node.get("auditId");
            if (idNode == null || idNode.isNull()) {
                return null;
            }
            return UUID.fromString(idNode.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
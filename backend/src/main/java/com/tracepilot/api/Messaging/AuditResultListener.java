package com.tracepilot.api.Messaging;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.Entities.AgentReport;
import com.tracepilot.api.Entities.ReliabilityHistory;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Enums.TypesOfAgent;
import com.tracepilot.api.Services.AuditEmitterRegistry;
import com.tracepilot.api.Repositories.AgentReportRepository;
import com.tracepilot.api.Repositories.ReliabilityHistoryRepository;
import com.tracepilot.api.Repositories.TraceAuditRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditResultListener {

    private final TraceAuditRepository auditRepository;
    private final AgentReportRepository agentReportRepository;
    private final ReliabilityHistoryRepository reliabilityHistoryRepository;
    private final AuditEmitterRegistry auditEmitterRegistry;
    private final ObjectMapper objectMapper;

    public AuditResultListener(TraceAuditRepository auditRepository,
            AgentReportRepository agentReportRepository,
            ReliabilityHistoryRepository reliabilityHistoryRepository,
            AuditEmitterRegistry auditEmitterRegistry,
            ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.agentReportRepository = agentReportRepository;
        this.reliabilityHistoryRepository = reliabilityHistoryRepository;
        this.auditEmitterRegistry = auditEmitterRegistry;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "audit.results")
    @Transactional
    public void handleAuditResult(Message message) {
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getBody());
        } catch (Exception e) {
            log.error("Malformed audit.results message, discarding. Raw body: {}",
                    new String(message.getBody(), StandardCharsets.UTF_8));
            throw new AmqpRejectAndDontRequeueException("Malformed audit.results message", e);
        }

        UUID auditId;
        try {
            auditId = UUID.fromString(root.path("auditId").asText());
        } catch (Exception e) {
            log.error("audit.results message missing/invalid auditId, discarding: {}", root);
            throw new AmqpRejectAndDontRequeueException("Missing or invalid auditId", e);
        }

        TraceAudit audit = auditRepository.findById(auditId).orElse(null);
        if (audit == null) {
            log.error("audit.results referenced auditId={} but no TraceAudit row exists.", auditId);
            throw new AmqpRejectAndDontRequeueException("No TraceAudit row for auditId=" + auditId);
        }

        if (audit.getStatus() == AuditStatus.COMPLETE || audit.getStatus() == AuditStatus.FAILED) {
            log.info("Ignoring duplicate result for already finalized audit {}", auditId);
            return;
        }

        String status = root.path("status").asText("");

        if ("FAILED".equals(status)) {
            String workerError = root.path("error").asText("Worker reported failure.");
            audit.setStatus(AuditStatus.FAILED);
            audit.setFailureReason(workerError);
            auditRepository.save(audit);
            auditEmitterRegistry.pushAndComplete(auditId, AuditResponse.from(audit));
            log.error("Audit {} marked FAILED. Worker error: {}", auditId, root.path("error").asText(""));
            return;
        }

        if (!"COMPLETE".equals(status)) {
            log.error("Audit {} received unrecognized status '{}' on audit.results, discarding.", auditId, status);
            throw new AmqpRejectAndDontRequeueException("Unrecognized status: " + status);
        }

        JsonNode report = root.get("report");
        if (report == null || report.isNull()) {
            log.error("Audit {} marked COMPLETE but has no report payload, discarding.", auditId);
            throw new AmqpRejectAndDontRequeueException("Missing report payload for auditId=" + auditId);
        }

        persistCompletedReport(audit, report);
    }

    private void persistCompletedReport(TraceAudit audit, JsonNode report) {
        JsonNode processingTimes = report.path("processingTimeMs");

        saveAgentReport(audit, TypesOfAgent.TRACE_LOOP_EFFICIENCY,
                report.get("loopEfficiencyReport"), processingTimes.path("loop_efficiency"));
        saveAgentReport(audit, TypesOfAgent.BLIND_OUTCOME_VERIFIER,
                report.get("blindOutcomeReport"), processingTimes.path("blind_outcome"));
        saveAgentReport(audit, TypesOfAgent.RELIABILITY_TREND,
                report.get("reliabilityTrendReport"), processingTimes.path("reliability_trend"));

        JsonNode reliabilityBlock = report.get("reliabilityTrendReport");
        int reliabilityScore = reliabilityBlock.path("current_reliability_score").asInt(0);

        ReliabilityHistory history = new ReliabilityHistory();
        history.setUser(audit.getUser());
        history.setAudit(audit);
        history.setRepoName(audit.getRepoName());
        history.setAgentTool(audit.getAgentTool());
        history.setReliabilityScore(reliabilityScore);
        history.setSignalSummary(reliabilityBlock.toString());
        history.setRecordedAt(Instant.now());
        try {
            reliabilityHistoryRepository.saveAndFlush(history);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate ReliabilityHistory for audit {}, ignoring.", audit.getId());
        }

        audit.setOverallScore(report.path("overallScore").asInt(0));
        audit.setExtractedEvidence(report.path("extractedEvidence").toString());
        audit.setWithheldClaims(report.path("withheldClaims").toString());
        audit.setStatus(AuditStatus.COMPLETE);
        audit.setCompletedAt(Instant.now());
        auditRepository.save(audit);
        auditEmitterRegistry.pushAndComplete(audit.getId(), AuditResponse.from(audit));

        log.info("Audit {} completed and persisted.", audit.getId());
    }

    private void saveAgentReport(TraceAudit audit, TypesOfAgent type,
            JsonNode findingsNode, JsonNode processingTimeNode) {
        if (findingsNode == null || findingsNode.isNull()) {
            log.error("Audit {} missing {} report block, skipping that agent report.", audit.getId(), type);
            return;
        }
        AgentReport agentReport = new AgentReport();
        agentReport.setAudit(audit);
        agentReport.setAgentType(type);
        agentReport.setFindings(findingsNode.toString());
        agentReport.setSeverityScore(findingsNode.path("severity_score").asInt(0));
        if (processingTimeNode != null && processingTimeNode.isInt()) {
            agentReport.setProcessingTimeMs(processingTimeNode.asInt());
        }
        try {
            agentReportRepository.saveAndFlush(agentReport);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate AgentReport for audit {} / type {}, ignoring.", audit.getId(), type);
        }
    }
}
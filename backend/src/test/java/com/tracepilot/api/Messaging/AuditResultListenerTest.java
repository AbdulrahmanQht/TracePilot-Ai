package com.tracepilot.api.Messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataIntegrityViolationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.Entities.AgentReport;
import com.tracepilot.api.Entities.ReliabilityHistory;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Enums.TypesOfAgent;
import com.tracepilot.api.Repositories.AgentReportRepository;
import com.tracepilot.api.Repositories.ReliabilityHistoryRepository;
import com.tracepilot.api.Repositories.TraceAuditRepository;

@ExtendWith(MockitoExtension.class)
class AuditResultListenerTest {

    @Mock
    private TraceAuditRepository auditRepository;
    @Mock
    private AgentReportRepository agentReportRepository;
    @Mock
    private ReliabilityHistoryRepository reliabilityHistoryRepository;

    private AuditResultListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuditResultListener(auditRepository, agentReportRepository, reliabilityHistoryRepository,
                new ObjectMapper());
    }

    private Message messageWithBody(String json) {
        return new Message(json.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }

    private TraceAudit pendingAudit(UUID id) {
        TraceAudit audit = new TraceAudit();
        audit.setId(id);
        audit.setStatus(AuditStatus.PENDING);
        User user = new User();
        user.setId(UUID.randomUUID());
        audit.setUser(user);
        audit.setRepoName("tracepilot");
        audit.setAgentTool("GENERIC");
        return audit;
    }

    private String completeReportJson(UUID auditId) {
        return """
                {
                    "auditId": "%s",
                    "status": "COMPLETE",
                    "report": {
                        "overallScore": 82,
                        "extractedEvidence": { "note": "evidence" },
                        "withheldClaims": { "note": "claims" },
                        "loopEfficiencyReport": { "severity_score": 10, "detail": "loop" },
                        "blindOutcomeReport": { "severity_score": 20, "detail": "outcome" },
                        "reliabilityTrendReport": { "severity_score": 5, "current_reliability_score": 91 },
                        "processingTimeMs": {
                            "loop_efficiency": 120,
                            "blind_outcome": 340,
                            "reliability_trend": 75
                        }
                    }
                }
                """.formatted(auditId);
    }

    @Test
    void handleAuditResult_throwsAndDiscards_whenPayloadIsMalformedJson() {
        assertThatThrownBy(() -> listener.handleAuditResult(messageWithBody("not-json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void handleAuditResult_throwsAndDiscards_whenAuditIdMissingOrInvalid() {
        assertThatThrownBy(() -> listener.handleAuditResult(messageWithBody("{\"status\": \"COMPLETE\"}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void handleAuditResult_throwsAndDiscards_whenNoMatchingAuditExists() {
        UUID auditId = UUID.randomUUID();
        when(auditRepository.findById(auditId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.handleAuditResult(
                messageWithBody("{\"auditId\": \"" + auditId + "\", \"status\": \"COMPLETE\"}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void handleAuditResult_ignoresMessage_whenAuditAlreadyComplete() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        audit.setStatus(AuditStatus.COMPLETE);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        listener.handleAuditResult(messageWithBody("{\"auditId\": \"" + auditId + "\", \"status\": \"COMPLETE\"}"));

        verify(auditRepository, never()).save(any());
        verify(agentReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void handleAuditResult_ignoresMessage_whenAuditAlreadyFailed() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        audit.setStatus(AuditStatus.FAILED);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        listener.handleAuditResult(messageWithBody("{\"auditId\": \"" + auditId + "\", \"status\": \"COMPLETE\"}"));

        verify(auditRepository, never()).save(any());
    }

    @Test
    void handleAuditResult_marksAuditFailed_whenWorkerReportsFailure() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        listener.handleAuditResult(messageWithBody(
                "{\"auditId\": \"" + auditId + "\", \"status\": \"FAILED\", \"error\": \"LLM timeout\"}"));

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.FAILED);
        verify(auditRepository).save(audit);
        verify(agentReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void handleAuditResult_throwsAndDiscards_whenStatusIsUnrecognized() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> listener.handleAuditResult(
                messageWithBody("{\"auditId\": \"" + auditId + "\", \"status\": \"WEIRD_STATUS\"}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void handleAuditResult_throwsAndDiscards_whenCompleteButReportMissing() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> listener.handleAuditResult(
                messageWithBody("{\"auditId\": \"" + auditId + "\", \"status\": \"COMPLETE\"}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void handleAuditResult_persistsAgentReportsHistoryAndAudit_whenReportComplete() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        listener.handleAuditResult(messageWithBody(completeReportJson(auditId)));

        ArgumentCaptor<AgentReport> reportCaptor = ArgumentCaptor.forClass(AgentReport.class);
        verify(agentReportRepository, times(3)).saveAndFlush(reportCaptor.capture());
        assertThat(reportCaptor.getAllValues())
                .extracting(AgentReport::getAgentType)
                .containsExactlyInAnyOrder(
                        TypesOfAgent.TRACE_LOOP_EFFICIENCY,
                        TypesOfAgent.BLIND_OUTCOME_VERIFIER,
                        TypesOfAgent.RELIABILITY_TREND);

        ArgumentCaptor<ReliabilityHistory> historyCaptor = ArgumentCaptor.forClass(ReliabilityHistory.class);
        verify(reliabilityHistoryRepository).saveAndFlush(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getReliabilityScore()).isEqualTo(91);

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETE);
        assertThat(audit.getOverallScore()).isEqualTo(82);
        assertThat(audit.getCompletedAt()).isNotNull();
        verify(auditRepository).save(audit);
    }

    @Test
    void handleAuditResult_skipsMissingAgentReportBlocks_withoutFailing() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));

        String json = """
                {
                    "auditId": "%s",
                    "status": "COMPLETE",
                    "report": {
                        "overallScore": 50,
                        "reliabilityTrendReport": { "severity_score": 5, "current_reliability_score": 60 }
                    }
                }
                """.formatted(auditId);

        listener.handleAuditResult(messageWithBody(json));

        // Only the reliability-trend agent report block was present.
        verify(agentReportRepository, times(1)).saveAndFlush(any());
        verify(reliabilityHistoryRepository).saveAndFlush(any());
        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETE);
    }

    @Test
    void handleAuditResult_ignoresDuplicateAgentReport_onUniqueConstraintViolation() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(agentReportRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate audit_id + agent_type"));

        org.assertj.core.api.Assertions.assertThatCode(
                () -> listener.handleAuditResult(messageWithBody(completeReportJson(auditId))))
                .doesNotThrowAnyException();

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETE);
    }

    @Test
    void handleAuditResult_ignoresDuplicateReliabilityHistory_onUniqueConstraintViolation() {
        UUID auditId = UUID.randomUUID();
        TraceAudit audit = pendingAudit(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(reliabilityHistoryRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate reliability history"));

        org.assertj.core.api.Assertions.assertThatCode(
                () -> listener.handleAuditResult(messageWithBody(completeReportJson(auditId))))
                .doesNotThrowAnyException();

        assertThat(audit.getStatus()).isEqualTo(AuditStatus.COMPLETE);
        verify(auditRepository).save(audit);
    }
}

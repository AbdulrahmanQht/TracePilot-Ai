package com.tracepilot.api.DTO;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tracepilot.api.DTO.Response.AgentReportResponse;
import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.DTO.Response.ReliabilityResponse;
import com.tracepilot.api.Entities.AgentReport;
import com.tracepilot.api.Entities.ReliabilityHistory;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Enums.TypesOfAgent;

class DtoMappingTest {

    @Test
    void agentReportResponse_from_mapsAllFields() {
        AgentReport report = new AgentReport();
        report.setId(UUID.randomUUID());
        report.setAgentType(TypesOfAgent.BLIND_OUTCOME_VERIFIER);
        report.setFindings("{\"severity_score\":42}");
        report.setSeverityScore(42);
        report.setProcessingTimeMs(150);

        AgentReportResponse response = AgentReportResponse.from(report);

        assertThat(response.id()).isEqualTo(report.getId());
        assertThat(response.agentType()).isEqualTo(TypesOfAgent.BLIND_OUTCOME_VERIFIER);
        assertThat(response.findings()).isEqualTo("{\"severity_score\":42}");
        assertThat(response.severityScore()).isEqualTo(42);
        assertThat(response.processingTimeMs()).isEqualTo(150);
    }

    @Test
    void reliabilityResponse_from_mapsAllFields() {
        ReliabilityHistory history = new ReliabilityHistory();
        history.setId(UUID.randomUUID());
        history.setRepoName("tracepilot");
        history.setAgentTool("GENERIC");
        history.setReliabilityScore(75);
        history.setSignalSummary("{}");
        Instant recordedAt = Instant.now();
        history.setRecordedAt(recordedAt);

        ReliabilityResponse response = ReliabilityResponse.from(history);

        assertThat(response.id()).isEqualTo(history.getId());
        assertThat(response.repoName()).isEqualTo("tracepilot");
        assertThat(response.agentTool()).isEqualTo("GENERIC");
        assertThat(response.reliabilityScore()).isEqualTo(75);
        assertThat(response.recordedAt()).isEqualTo(recordedAt);
    }

    @Test
    void auditResponse_from_mapsScalarFieldsAndNestedAgentReports() {
        User user = new User();
        user.setId(UUID.randomUUID());

        TraceAudit audit = new TraceAudit();
        audit.setId(UUID.randomUUID());
        audit.setUser(user);
        audit.setTitle("My Audit");
        audit.setRepoName("tracepilot");
        audit.setAgentTool("GENERIC");
        audit.setInputSource(AuditInputSource.CI_LOG);
        audit.setStatus(AuditStatus.COMPLETE);
        audit.setOverallScore(77);
        audit.setIsPublic(true);
        audit.setShareToken("share-token");

        AgentReport report = new AgentReport();
        report.setId(UUID.randomUUID());
        report.setAudit(audit);
        report.setAgentType(TypesOfAgent.TRACE_LOOP_EFFICIENCY);
        report.setFindings("{}");
        report.setSeverityScore(10);
        audit.setAgentReports(List.of(report));

        AuditResponse response = AuditResponse.from(audit);

        assertThat(response.id()).isEqualTo(audit.getId());
        assertThat(response.title()).isEqualTo("My Audit");
        assertThat(response.repoName()).isEqualTo("tracepilot");
        assertThat(response.inputSource()).isEqualTo(AuditInputSource.CI_LOG);
        assertThat(response.status()).isEqualTo(AuditStatus.COMPLETE);
        assertThat(response.overallScore()).isEqualTo(77);
        assertThat(response.isPublic()).isTrue();
        assertThat(response.shareToken()).isEqualTo("share-token");
        assertThat(response.reports()).hasSize(1);
        assertThat(response.reports().get(0).agentType()).isEqualTo(TypesOfAgent.TRACE_LOOP_EFFICIENCY);
    }

    @Test
    void auditResponse_from_handlesEmptyAgentReportsList() {
        User user = new User();
        user.setId(UUID.randomUUID());
        TraceAudit audit = new TraceAudit();
        audit.setId(UUID.randomUUID());
        audit.setUser(user);

        AuditResponse response = AuditResponse.from(audit);

        assertThat(response.reports()).isEmpty();
    }
}

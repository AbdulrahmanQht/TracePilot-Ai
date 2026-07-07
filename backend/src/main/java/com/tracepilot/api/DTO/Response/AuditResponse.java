package com.tracepilot.api.DTO.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Enums.AuditStatus;

public record AuditResponse(
        UUID id,
        String title,
        String repoName,
        String agentTool,
        AuditInputSource inputSource,
        AuditStatus status,
        Integer overallScore,
        boolean isPublic,
        String shareToken,
        Instant createdAt,
        Instant completedAt,
        List<AgentReportResponse> reports) {

    public static AuditResponse from(TraceAudit audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getTitle(),
                audit.getRepoName(),
                audit.getAgentTool(),
                audit.getInputSource(),
                audit.getStatus(),
                audit.getOverallScore(),
                audit.getIsPublic(),
                audit.getShareToken(),
                audit.getCreatedAt(),
                audit.getCompletedAt(),
                audit.getAgentReports().stream().map(AgentReportResponse::from).toList());
    }
}

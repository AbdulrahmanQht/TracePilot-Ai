package com.tracepilot.api.DTO.Response;

import java.time.Instant;
import java.util.UUID;

import com.tracepilot.api.Entities.AgentReport;
import com.tracepilot.api.Enums.TypesOfAgent;

public record AgentReportResponse(
        UUID id,
        TypesOfAgent agentType,
        String findings,
        int severityScore,
        Integer processingTimeMs,
        Instant createdAt) {

    public static AgentReportResponse from(AgentReport report) {
        return new AgentReportResponse(
                report.getId(),
                report.getAgentType(),
                report.getFindings(),
                report.getSeverityScore(),
                report.getProcessingTimeMs(),
                report.getCreatedAt());
    }
}

package com.tracepilot.api.DTO.Response;

import java.time.Instant;
import java.util.UUID;

import com.tracepilot.api.Entities.ReliabilityHistory;

public record ReliabilityResponse(
        UUID id,
        String repoName,
        String agentTool,
        int reliabilityScore,
        String signalSummary,
        Instant recordedAt) {

    public static ReliabilityResponse from(ReliabilityHistory history) {
        return new ReliabilityResponse(
                history.getId(),
                history.getRepoName(),
                history.getAgentTool(),
                history.getReliabilityScore(),
                history.getSignalSummary(),
                history.getRecordedAt());
    }
}
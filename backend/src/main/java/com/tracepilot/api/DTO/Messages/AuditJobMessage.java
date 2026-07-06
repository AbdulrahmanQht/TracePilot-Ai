package com.tracepilot.api.DTO.Messages;

import java.util.List;
import java.util.UUID;

public record AuditJobMessage(
        UUID auditId,
        UUID userId,
        String title,
        String rawTrace,
        String repoName,
        String agentTool,
        String inputSource,
                        
        List<PriorReliability> priorHistory) {
        public record PriorReliability(
                int reliabilityScore,
                String signalSummary,
                String recordedAt) {
        }
}
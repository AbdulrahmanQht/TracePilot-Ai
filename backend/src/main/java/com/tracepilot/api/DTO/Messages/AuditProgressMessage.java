package com.tracepilot.api.DTO.Messages;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.tracepilot.api.Enums.TypesOfAgent;
import com.tracepilot.api.Enums.AuditProgressStatus;

public record AuditProgressMessage(
        @JsonAlias("audit_id")
        UUID auditId,
        @JsonAlias("agent_type")
        TypesOfAgent agentType,
        String step,
        AuditProgressStatus status,
        String message) {
}

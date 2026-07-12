package com.tracepilot.api.DTO.Messages;

import java.util.UUID;

import com.tracepilot.api.Enums.TypesOfAgent;
import com.tracepilot.api.Enums.AuditProgressStatus;

public record AuditProgressMessage(
        UUID auditId,
        TypesOfAgent agentType,
        String step,
        AuditProgressStatus status,
        String message) {
}

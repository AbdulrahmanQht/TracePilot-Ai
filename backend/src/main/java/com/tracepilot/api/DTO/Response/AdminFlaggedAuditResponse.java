package com.tracepilot.api.DTO.Response;

import java.util.UUID;
import java.time.Instant;

import com.tracepilot.api.Enums.AuditStatus;

public record AdminFlaggedAuditResponse(
    UUID id,
    AuditStatus status,
    boolean suspicious,
    Instant createdAt,
    AdminUserSummary user
) {}

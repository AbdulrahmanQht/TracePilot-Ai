package com.tracepilot.api.DTO.Response;

import java.util.UUID;
import java.time.Instant;

import com.tracepilot.api.Enums.UserRoles;

public record AdminUserResponse(
        UUID id,
        String displayName,
        String email,
        UserRoles role,
        boolean verified,
        Instant createdAt,
        int auditCountToday) {
}

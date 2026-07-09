package com.tracepilot.api.DTO.Response;

import java.util.UUID;

public record AdminUserSummary(
        UUID id,
        String displayName,
        String email) {
}

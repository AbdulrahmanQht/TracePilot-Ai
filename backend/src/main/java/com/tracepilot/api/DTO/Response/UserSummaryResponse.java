package com.tracepilot.api.DTO.Response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        String displayName,
        String role
){}

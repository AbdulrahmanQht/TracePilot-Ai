package com.tracepilot.api.DTO.Response;

import java.time.Instant;
import com.tracepilot.api.DTO.Response.UserSummaryResponse;

public record AuthResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    UserSummaryResponse user
) {}

package com.tracepilot.api.Services;

import com.tracepilot.api.DTO.Response.AuthResponse;

public record AuthResult(
        AuthResponse response,
        String refreshToken) {
}

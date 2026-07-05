package com.tracepilot.api.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tracepilot.jwt")
public record JwtConfig(
        String secret,
        long accessExpiryMs,
        int refreshExpiryDays) {
}

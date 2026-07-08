package com.tracepilot.api.Config;

import java.util.UUID;
import java.time.Duration;

import com.tracepilot.api.Services.RateLimiterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiterService<UUID> auditRateLimiter() {
        return new RateLimiterService<>(10, Duration.ofDays(1));
    }

    @Bean
    public RateLimiterService<String> authRateLimiter() {
        return new RateLimiterService<>(5, Duration.ofMinutes(15));
    }
}
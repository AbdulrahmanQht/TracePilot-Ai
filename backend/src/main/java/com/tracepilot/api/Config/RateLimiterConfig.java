package com.tracepilot.api.Config;

import java.time.Duration;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tracepilot.api.Services.RateLimiterService;

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

    @Bean
    public RateLimiterService<String> refreshRateLimiter() {
        return new RateLimiterService<>(20, Duration.ofMinutes(15));
    }

    @Bean
    public RateLimiterService<String> sharedReportRateLimiter() {
        return new RateLimiterService<>(60, Duration.ofMinutes(10));
    }
}
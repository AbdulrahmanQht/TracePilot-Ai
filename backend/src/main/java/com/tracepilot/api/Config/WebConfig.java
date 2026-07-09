package com.tracepilot.api.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tracepilot.api.Security.AuthRateLimitInterceptor;
import com.tracepilot.api.Security.RateLimitInterceptor;
import com.tracepilot.api.Security.RefreshRateLimitInterceptor;
import com.tracepilot.api.Security.SharedReportRateLimitInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuthRateLimitInterceptor authRateLimitInterceptor;
    private final RefreshRateLimitInterceptor refreshRateLimitInterceptor;
    private final SharedReportRateLimitInterceptor sharedReportRateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor,
            AuthRateLimitInterceptor authRateLimitInterceptor,
            RefreshRateLimitInterceptor refreshRateLimitInterceptor,
            SharedReportRateLimitInterceptor sharedReportRateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.authRateLimitInterceptor = authRateLimitInterceptor;
        this.refreshRateLimitInterceptor = refreshRateLimitInterceptor;
        this.sharedReportRateLimitInterceptor = sharedReportRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Enforce rate limits exclusively on endpoints managing audit data
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/audits/**");

        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/verify-email",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/resend-verification");

        registry.addInterceptor(refreshRateLimitInterceptor)
                .addPathPatterns("/api/v1/auth/refresh");

        registry.addInterceptor(sharedReportRateLimitInterceptor)
                .addPathPatterns("/api/v1/shared/**");
    }
}
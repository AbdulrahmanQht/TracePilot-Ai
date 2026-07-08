package com.tracepilot.api.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.tracepilot.api.Security.AuthRateLimitInterceptor;
import com.tracepilot.api.Security.RateLimitInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final AuthRateLimitInterceptor authRateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor, AuthRateLimitInterceptor authRateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.authRateLimitInterceptor = authRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Enforce rate limits exclusively on endpoints managing audit data
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/audits/**");

        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns("/api/v1/auth/login", "/api/v1/auth/register");
    }
}
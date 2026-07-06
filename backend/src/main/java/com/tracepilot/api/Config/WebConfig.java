package com.tracepilot.api.Config;

import com.tracepilot.api.Security.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Enforce rate limits exclusively on endpoints managing audit data
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/audits/**");
    }
}
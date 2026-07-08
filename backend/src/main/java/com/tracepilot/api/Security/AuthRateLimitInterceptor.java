package com.tracepilot.api.Security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.tracepilot.api.Services.RateLimiterService;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiterService<String> rateLimiterService;

    public AuthRateLimitInterceptor(@Qualifier("authRateLimiter") RateLimiterService<String> rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        String clientIp = request.getRemoteAddr();
        Bucket bucket = rateLimiterService.resolveBucket(clientIp);

        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("Auth rate limit breached from IP: {} on endpoint: {}", clientIp, request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter()
                .write("{\"error\": \"Too Many Requests\", \"message\": \"Too many attempts, try again later.\"}");

        return false;
    }
}
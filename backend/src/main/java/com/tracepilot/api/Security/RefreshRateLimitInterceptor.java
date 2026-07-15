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
public class RefreshRateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiterService<String> rateLimiterService;

    public RefreshRateLimitInterceptor(@Qualifier("refreshRateLimiter") RateLimiterService<String> rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String clientIp = IPResolver.getClientIp(request);
        if (IPResolver.isLocalhost(clientIp)) {
            return true;
        }
        Bucket bucket = rateLimiterService.resolveBucket(clientIp);

        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("Refresh rate limit breached from IP: {}", clientIp);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter()
                .write("{\"error\": \"Too Many Requests\", \"message\": \"Too many refresh attempts, try again later.\"}");

        return false;
    }
}
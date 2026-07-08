package com.tracepilot.api.Security;

import java.util.UUID;

import com.tracepilot.api.Services.RateLimiterService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiterService<UUID> rateLimiterService;

    public RateLimitInterceptor(@Qualifier("auditRateLimiter") RateLimiterService<UUID> rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // If the route is anonymous or not fully populated yet, proceed safely
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            log.trace("Rate limiting skipped: Anonymous request to {}", request.getRequestURI());
            return true;
        }

        Bucket bucket = rateLimiterService.resolveBucket(principal.id());

        // Attempt to consume 1 token for the request
        if (bucket.tryConsume(1)) {
            log.debug("Rate limit token consumed for user: {}. Remaining tokens: {}", principal.id(),
                    bucket.getAvailableTokens());
            return true;
        }

        // Limit exceeded
        log.warn("Rate limit breached by user ID: {} on endpoint: {}", principal.id(), request.getRequestURI());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Daily audit quota exceeded.\"}");

        return false; // Halts further execution within the execution chain
    }
}

package com.tracepilot.api.Security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Services.RateLimiterService;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        RateLimiterService<UUID> rateLimiterService = new RateLimiterService<>(2, Duration.ofDays(1));
        interceptor = new RateLimitInterceptor(rateLimiterService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(AuthenticatedUser principal) {
        var authToken = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @Test
    void preHandle_allowsAnonymousRequests_whenNoAuthenticationPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/audits");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void preHandle_allowsRequests_withinDailyQuota() throws Exception {
        authenticateAs(new AuthenticatedUser(UUID.randomUUID(), "a@b.com", UserRoles.USER));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/audits");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void preHandle_blocksRequests_onceDailyQuotaExceeded() throws Exception {
        authenticateAs(new AuthenticatedUser(UUID.randomUUID(), "a@b.com", UserRoles.USER));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/audits");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        interceptor.preHandle(request, response, new Object());
        boolean thirdAttempt = interceptor.preHandle(request, response, new Object());

        assertThat(thirdAttempt).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentAsString()).contains("Daily audit quota exceeded");
    }

    @Test
    void preHandle_tracksQuotaIndependently_perUser() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticateAs(new AuthenticatedUser(UUID.randomUUID(), "userA@b.com", UserRoles.USER));
        MockHttpServletRequest requestA = new MockHttpServletRequest("POST", "/api/v1/audits");
        interceptor.preHandle(requestA, response, new Object());
        interceptor.preHandle(requestA, response, new Object());

        authenticateAs(new AuthenticatedUser(UUID.randomUUID(), "userB@b.com", UserRoles.USER));
        MockHttpServletRequest requestB = new MockHttpServletRequest("POST", "/api/v1/audits");

        assertThat(interceptor.preHandle(requestB, response, new Object())).isTrue();
    }
}

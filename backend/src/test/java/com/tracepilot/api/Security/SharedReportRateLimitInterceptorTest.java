package com.tracepilot.api.Security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.tracepilot.api.Services.RateLimiterService;

class SharedReportRateLimitInterceptorTest {

    private SharedReportRateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        RateLimiterService<String> rateLimiterService = new RateLimiterService<>(2, Duration.ofMinutes(15));
        interceptor = new SharedReportRateLimitInterceptor(rateLimiterService);
    }

    @Test
    void preHandle_allowsRequests_withinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/shared/abc123");
        request.setRemoteAddr("10.0.2.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void preHandle_blocksRequests_onceLimitExceeded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/shared/abc123");
        request.setRemoteAddr("10.0.2.2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        interceptor.preHandle(request, response, new Object());
        boolean thirdAttempt = interceptor.preHandle(request, response, new Object());

        assertThat(thirdAttempt).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void preHandle_tracksLimitsIndependently_perClientIp() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpServletRequest requestA = new MockHttpServletRequest("GET", "/api/v1/shared/abc123");
        requestA.setRemoteAddr("10.0.2.10");
        interceptor.preHandle(requestA, response, new Object());
        interceptor.preHandle(requestA, response, new Object());

        MockHttpServletRequest requestB = new MockHttpServletRequest("GET", "/api/v1/shared/abc123");
        requestB.setRemoteAddr("10.0.2.11");

        assertThat(interceptor.preHandle(requestB, response, new Object())).isTrue();
    }
}

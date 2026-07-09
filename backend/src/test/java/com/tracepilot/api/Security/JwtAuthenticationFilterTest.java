package com.tracepilot.api.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Services.JwtService;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_skipsAuthentication_whenNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audits");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void doFilterInternal_skipsAuthentication_whenHeaderDoesNotStartWithBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audits");
        request.addHeader("Authorization", "Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_setsAuthentication_whenTokenIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audits");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.validateAccessToken("valid-token")).thenReturn(claims);
        when(jwtService.extractUserId(claims)).thenReturn(userId);
        when(jwtService.extractEmail(claims)).thenReturn("abdulrahman@example.com");
        when(jwtService.extractRole(claims)).thenReturn(UserRoles.USER);

        filter.doFilterInternal(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        assertThat(principal.id()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("abdulrahman@example.com");
        assertThat(principal.role()).isEqualTo(UserRoles.USER);

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void doFilterInternal_clearsContext_whenTokenValidationFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audits");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.validateAccessToken("invalid-token"))
                .thenThrow(new ApiException("Invalid access token", HttpStatus.UNAUTHORIZED));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_alwaysContinuesFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/audits");
        MockHttpServletResponse response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = org.mockito.Mockito.mock(jakarta.servlet.FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}

package com.tracepilot.api.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import com.tracepilot.api.Config.JwtConfig;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.OAuthProvider;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Services.AuthService;
import com.tracepilot.api.Services.JwtService;
import com.tracepilot.api.Services.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private AuthService authService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig("secret", 900_000L, 7, true);
        handler = new OAuth2LoginSuccessHandler(authService, jwtService, jwtConfig, refreshTokenService);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:5173");
    }

    private OAuth2User oAuth2User(Map<String, Object> attributes, String nameAttributeKey) {
        return new DefaultOAuth2User(java.util.List.of(() -> "ROLE_USER"), attributes, nameAttributeKey);
    }

    @Test
    void onAuthenticationSuccess_processesGoogleLogin_andRedirectsWithToken() throws Exception {
        OAuth2User principal = oAuth2User(Map.of(
                "sub", "google-sub-123",
                "email", "abdulrahman@example.com",
                "name", "Abdulrahman"), "sub");
        var token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        User resolvedUser = new User();
        resolvedUser.setId(UUID.randomUUID());
        resolvedUser.setEmail("abdulrahman@example.com");
        resolvedUser.setRole(UserRoles.USER);

        when(authService.processOAuthAfterLogin("abdulrahman@example.com", "Abdulrahman", OAuthProvider.GOOGLE,
                "google-sub-123")).thenReturn(resolvedUser);
        when(jwtService.generateAccessToken(resolvedUser)).thenReturn("jwt-access-token");
        when(refreshTokenService.issue(resolvedUser)).thenReturn("raw-refresh-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        assertThat(response.getRedirectedUrl())
                .startsWith("http://localhost:5173/oauth2/redirect")
                .contains("token=jwt-access-token");
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=raw-refresh-token");
    }

    @Test
    void onAuthenticationSuccess_fallsBackToGeneratedEmail_whenGithubOmitsEmail() throws Exception {
        OAuth2User principal = oAuth2User(Map.of(
                "id", 12345,
                "login", "abdulrahmanqht"), "id");
        var token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");

        User resolvedUser = new User();
        resolvedUser.setId(UUID.randomUUID());
        resolvedUser.setEmail("abdulrahmanqht@github.com");

        when(authService.processOAuthAfterLogin(eq("abdulrahmanqht@github.com"), any(), eq(OAuthProvider.GITHUB),
                any())).thenReturn(resolvedUser);
        when(jwtService.generateAccessToken(resolvedUser)).thenReturn("jwt-access-token");
        when(refreshTokenService.issue(resolvedUser)).thenReturn("raw-refresh-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        verify(authService).processOAuthAfterLogin(eq("abdulrahmanqht@github.com"), any(), eq(OAuthProvider.GITHUB),
                any());
        assertThat(response.getRedirectedUrl()).contains("token=jwt-access-token");
    }

    @Test
    void onAuthenticationSuccess_resolvesGithubProvider_forGithubRegistrationId() throws Exception {
        OAuth2User principal = oAuth2User(Map.of(
                "id", 54321,
                "login", "someone",
                "email", "someone@example.com"), "id");
        var token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");

        User resolvedUser = new User();
        resolvedUser.setId(UUID.randomUUID());
        resolvedUser.setEmail("someone@example.com");

        when(authService.processOAuthAfterLogin(eq("someone@example.com"), any(), eq(OAuthProvider.GITHUB), any()))
                .thenReturn(resolvedUser);
        when(jwtService.generateAccessToken(resolvedUser)).thenReturn("token-val");
        when(refreshTokenService.issue(resolvedUser)).thenReturn("refresh-val");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, token);

        verify(authService).processOAuthAfterLogin(any(), any(), eq(OAuthProvider.GITHUB), any());
    }
}

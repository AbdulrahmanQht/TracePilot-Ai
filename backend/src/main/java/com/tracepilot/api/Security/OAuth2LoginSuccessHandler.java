package com.tracepilot.api.Security;

import java.time.Duration;

import com.tracepilot.api.Entities.RefreshToken;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.OAuthProvider;
import com.tracepilot.api.Services.AuthService;
import com.tracepilot.api.Services.JwtService;
import com.tracepilot.api.Services.RefreshTokenService;
import com.tracepilot.api.Config.JwtConfig;

import jakarta.servlet.ServletException;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;

    @Value("${tracepilot.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(AuthService authService, JwtService jwtService, JwtConfig jwtConfig,
            RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.debug("OAuth2 authentication succeeded, processing login");
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();

        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuthProvider provider = registrationId.equalsIgnoreCase("github") ? OAuthProvider.GITHUB
                : OAuthProvider.GOOGLE;

        log.debug("OAuth provider resolved to {}", provider);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // GitHub sometimes doesn't return email in the principal if the user set it to
        // private
        if (email == null && provider == OAuthProvider.GITHUB) {
            log.warn("GitHub did not provide an email address. Using fallback email based on login.");
            email = oAuth2User.getAttribute("login") + "@github.com"; // Fallback
        }

        String oauthId = oAuth2User.getName();
        log.debug("Processing OAuth login for provider {} and OAuth ID {}", provider, oauthId);

        User user = authService.processOAuthAfterLogin(email, name, provider, oauthId);

        log.info("OAuth authentication successful for user ID {}", user.getId());

        log.debug("Generating access token and refresh token for user {}", user.getId());

        String jwt = jwtService.generateAccessToken(user);
        String rawRefreshToken = refreshTokenService.issue(user);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(rawRefreshToken).toString());

        log.debug("Refresh token cookie added to response for user {}", user.getId());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", jwt)
                .build().toUriString();

        log.debug("Redirecting user {} to frontend OAuth callback", user.getId());
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private ResponseCookie buildCookie(String rawToken) {
        log.debug("Building secure refresh token cookie");
        return ResponseCookie.from("refreshToken", rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(jwtConfig.refreshExpiryDays()))
                .build();
    }
}

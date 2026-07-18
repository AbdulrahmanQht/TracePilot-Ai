package com.tracepilot.api.Controllers;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.beans.factory.annotation.Value;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Config.JwtConfig;
import com.tracepilot.api.DTO.Request.LoginRequest;
import com.tracepilot.api.DTO.Request.RegisterRequest;
import com.tracepilot.api.DTO.Request.ForgotPasswordRequest;
import com.tracepilot.api.DTO.Request.ResetPasswordRequest;
import com.tracepilot.api.DTO.Request.ResendVerificationRequest;
import com.tracepilot.api.DTO.Request.ChangePasswordRequest;
import com.tracepilot.api.DTO.Response.AuthResponse;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Services.AuthService;
import com.tracepilot.api.Services.AuthResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "refreshToken";

    @Value("${tracepilot.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    public AuthController(AuthService authService, JwtConfig jwtConfig) {
        this.authService = authService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email={}", request.email());
        AuthResult result = authService.registerUser(request);

        log.info("User registered successfully: userId={}, email={}",
                result.response().user().id(),
                result.response().user().email());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email={}", request.email());
        AuthResult result = authService.loginUser(request);

        log.info("User logged in successfully: userId={}",
                result.response().user().id());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new ApiException("Missing refresh token", HttpStatus.UNAUTHORIZED);
        }
        log.debug("Refresh token request received");
        AuthResult result = authService.refreshToken(refreshToken);

        log.info("Access token refreshed for userId={}",
                result.response().user().id());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildCookie(result.refreshToken()).toString())
                .body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        log.info("Logout request received");
        if (refreshToken != null) {
            authService.logout(refreshToken);
            log.info("Refresh token revoked");
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
                .build();
    }

    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token, HttpServletResponse response) throws IOException {
        log.info("Email verification request received");
        boolean success;
        try {
            authService.verifyEmail(token);
            success = true;
        } catch (ApiException e) {
            success = false;
        }
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/email-verified")
                .queryParam("status", success ? "success" : "error")
                .build().toUriString();
        response.sendRedirect(targetUrl);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        log.info("Resend verification request received");

        authService.resendVerificationEmail(request.email());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot-password request received");
        authService.forgotPassword(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset-password request received");
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Authentication = {}", authentication);
        log.info("Principal class = {}", authentication == null ? null : authentication.getPrincipal().getClass());
        log.info("@AuthenticationPrincipal = {}", principal);
        log.info("Authenticated password change requested.");

        authService.changePassword(
                principal,
                request.currentPassword(),
                request.newPassword());

        return ResponseEntity.noContent().build();
    }

    private ResponseCookie buildCookie(String rawToken) {
        log.info("Cookie secure = {}", jwtConfig.cookieSecure());
        return ResponseCookie.from(REFRESH_COOKIE, rawToken)
                .httpOnly(true)
                .secure(jwtConfig.cookieSecure())
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(jwtConfig.refreshExpiryDays()))
                .build();
    }

    private ResponseCookie clearCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(true).sameSite("None").path("/api/v1/auth").maxAge(0).build();
    }
}

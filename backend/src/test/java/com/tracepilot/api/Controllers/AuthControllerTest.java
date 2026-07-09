package com.tracepilot.api.Controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.Config.JwtConfig;
import com.tracepilot.api.DTO.Request.ForgotPasswordRequest;
import com.tracepilot.api.DTO.Request.LoginRequest;
import com.tracepilot.api.DTO.Request.RegisterRequest;
import com.tracepilot.api.DTO.Request.ResetPasswordRequest;
import com.tracepilot.api.DTO.Response.AuthResponse;
import com.tracepilot.api.DTO.Response.UserSummaryResponse;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Exceptions.GlobalExceptionHandler;
import com.tracepilot.api.Services.AuthResult;
import com.tracepilot.api.Services.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig("secret", 900_000L, 7);
        AuthController controller = new AuthController(authService, jwtConfig);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuthResult sampleAuthResult() {
        UserSummaryResponse user = new UserSummaryResponse(UUID.randomUUID(), "abdulrahman@example.com",
                "Abdulrahman", "USER");
        AuthResponse response = new AuthResponse("access-token", "Bearer", Instant.now(), user);
        return new AuthResult(response, "raw-refresh-token");
    }

    @Test
    void register_returnsAuthResponse_andSetsRefreshCookie() throws Exception {
        RegisterRequest request = new RegisterRequest("abdulrahman@example.com", "password123", "Abdulrahman");
        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(sampleAuthResult());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void register_returns400_whenPasswordTooShort() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("abdulrahman@example.com", "short", "Abdulrahman");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns409_whenEmailAlreadyInUse() throws Exception {
        RegisterRequest request = new RegisterRequest("taken@example.com", "password123", "Name");
        when(authService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new ApiException("Email is already in use", org.springframework.http.HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    void login_returnsAuthResponse_andSetsRefreshCookie() throws Exception {
        LoginRequest request = new LoginRequest("abdulrahman@example.com", "password123");
        when(authService.loginUser(any(LoginRequest.class))).thenReturn(sampleAuthResult());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    void login_returns401_whenCredentialsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("abdulrahman@example.com", "wrong-password");
        when(authService.loginUser(any(LoginRequest.class)))
                .thenThrow(new ApiException("Invalid email or password",
                        org.springframework.http.HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns401_whenCookieMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing refresh token"));
    }

    @Test
    void refresh_returnsNewTokens_whenCookiePresent() throws Exception {
        when(authService.refreshToken("existing-refresh-token")).thenReturn(sampleAuthResult());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "existing-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void logout_revokesToken_whenCookiePresent_andClearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "existing-refresh-token")))
                .andExpect(status().isNoContent());

        verify(authService).logout("existing-refresh-token");
    }

    @Test
    void logout_succeeds_whenNoCookiePresent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());

        verify(authService, org.mockito.Mockito.never()).logout(any());
    }

    @Test
    void verifyEmail_returns200_whenTokenValid() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify-email").param("token", "valid-token"))
                .andExpect(status().isOk());

        verify(authService).verifyEmail("valid-token");
    }

    @Test
    void verifyEmail_returns400_whenTokenInvalid() throws Exception {
        org.mockito.Mockito.doThrow(new ApiException("Invalid or expired verification link",
                        org.springframework.http.HttpStatus.BAD_REQUEST))
                .when(authService).verifyEmail("bad-token");

        mockMvc.perform(get("/api/v1/auth/verify-email").param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_returns200_regardlessOfWhetherEmailExists() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("abdulrahman@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).forgotPassword("abdulrahman@example.com");
    }

    @Test
    void resetPassword_returns200_whenTokenAndPasswordValid() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "newPassword123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).resetPassword("reset-token", "newPassword123");
    }

    @Test
    void resetPassword_returns400_whenNewPasswordTooShort() throws Exception {
        ResetPasswordRequest invalidRequest = new ResetPasswordRequest("reset-token", "short");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}

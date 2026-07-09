package com.tracepilot.api.Controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tracepilot.api.DTO.Response.ReliabilityResponse;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Services.AuditService;

@ExtendWith(MockitoExtension.class)
class ReliabilityControllerTest {

    @Mock
    private AuditService auditService;

    private MockMvc mockMvc;
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        ReliabilityController controller = new ReliabilityController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        principal = new AuthenticatedUser(UUID.randomUUID(), "abdulrahman@example.com", UserRoles.USER);
        var authToken = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getReliabilityTrend_returnsHistoryForRequestedRepoAndTool() throws Exception {
        ReliabilityResponse history = new ReliabilityResponse(UUID.randomUUID(), "tracepilot", "GENERIC", 88,
                "{}", Instant.now());
        when(auditService.getReliabilityTrend(eq(principal), eq("tracepilot"), eq("GENERIC"), eq(10)))
                .thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/reliability")
                        .param("repoName", "tracepilot")
                        .param("agentTool", "GENERIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reliabilityScore").value(88));
    }

    @Test
    void getReliabilityTrend_usesProvidedLimit() throws Exception {
        when(auditService.getReliabilityTrend(eq(principal), eq("tracepilot"), eq("GENERIC"), eq(5)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reliability")
                        .param("repoName", "tracepilot")
                        .param("agentTool", "GENERIC")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getReliabilityTrend_returns400_whenRequiredParamsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/reliability"))
                .andExpect(status().isBadRequest());
    }
}

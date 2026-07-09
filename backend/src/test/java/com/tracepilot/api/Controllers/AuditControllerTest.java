package com.tracepilot.api.Controllers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.DTO.Request.AuditRequest;
import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Exceptions.GlobalExceptionHandler;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Services.AuditService;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        AuditController controller = new AuditController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(
                                        new AuthenticationPrincipalArgumentResolver(),
                                        new PageableHandlerMethodArgumentResolver())
                        .build();

        principal = new AuthenticatedUser(UUID.randomUUID(), "abdulrahman@example.com", UserRoles.USER);
        var authToken = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AuditResponse sampleAuditResponse(UUID id) {
        return new AuditResponse(id, "Title", "tracepilot", "GENERIC", AuditInputSource.PASTED_TEXT,
                AuditStatus.PENDING, null, false, null, Instant.now(), null, List.of());
    }

    @Test
    void getAudit_returnsAuditForOwner() throws Exception {
        UUID auditId = UUID.randomUUID();
        when(auditService.getAuditById(eq(auditId), eq(principal))).thenReturn(sampleAuditResponse(auditId));

        mockMvc.perform(get("/api/v1/audits/{id}", auditId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auditId.toString()));
    }

    @Test
    void getAudit_returns404_whenAuditNotFoundOrNotOwned() throws Exception {
        UUID auditId = UUID.randomUUID();
        when(auditService.getAuditById(eq(auditId), eq(principal)))
                .thenThrow(new ApiException("Audit not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/audits/{id}", auditId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Audit not found"));
    }

    @Test
    void listAudits_returnsPagedResults() throws Exception {
        UUID auditId = UUID.randomUUID();
        Page<AuditResponse> page = new PageImpl<>(List.of(sampleAuditResponse(auditId)), PageRequest.of(0, 20), 1);
        when(auditService.listAudits(eq(principal), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(auditId.toString()));
    }

    @Test
    void shareAudit_returnsShareableResponse() throws Exception {
        UUID auditId = UUID.randomUUID();
        AuditResponse shared = new AuditResponse(auditId, "Title", "tracepilot", "GENERIC",
                AuditInputSource.PASTED_TEXT, AuditStatus.COMPLETE, 90, true, "share-token-abc", Instant.now(),
                Instant.now(), List.of());
        when(auditService.createShareLink(eq(auditId), eq(principal))).thenReturn(shared);

        mockMvc.perform(post("/api/v1/audits/{id}/share", auditId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareToken").value("share-token-abc"))
                .andExpect(jsonPath("$.isPublic").value(true));
    }

    @Test
    void submitAudit_returns202Accepted_withCreatedAudit() throws Exception {
        AuditRequest request = new AuditRequest("trace content", "Title", "tracepilot", "GENERIC",
                AuditInputSource.PASTED_TEXT);
        UUID newAuditId = UUID.randomUUID();
        when(auditService.initiateAudit(any(AuditRequest.class), eq(principal)))
                .thenReturn(sampleAuditResponse(newAuditId));

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(newAuditId.toString()));
    }

    @Test
    void submitAudit_returns400_whenRawTraceIsBlank() throws Exception {
        AuditRequest invalidRequest = new AuditRequest("", "Title", "tracepilot", "GENERIC",
                AuditInputSource.PASTED_TEXT);

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}

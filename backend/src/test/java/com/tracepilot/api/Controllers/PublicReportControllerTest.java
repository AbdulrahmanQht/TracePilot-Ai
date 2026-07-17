package com.tracepilot.api.Controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Exceptions.GlobalExceptionHandler;
import com.tracepilot.api.Services.AuditService;

@ExtendWith(MockitoExtension.class)
class PublicReportControllerTest {

        @Mock
        private AuditService auditService;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                PublicReportController controller = new PublicReportController(auditService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .build();
        }

        @Test
        void getSharedReport_returnsAudit_whenTokenValid() throws Exception {
                UUID auditId = UUID.randomUUID();
                AuditResponse response = new AuditResponse(
                                auditId,
                                "Title",
                                "tracepilot",
                                "GENERIC",
                                AuditInputSource.PASTED_TEXT,
                                AuditStatus.COMPLETE,
                                85,
                                true,
                                "token-abc",
                                null, // failureReason
                                Instant.now(),
                                Instant.now(),
                                List.of());
                when(auditService.getSharedReport("token-abc")).thenReturn(response);

                mockMvc.perform(get("/api/v1/shared/{token}", "token-abc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.shareToken").value("token-abc"));
        }

        @Test
        void getSharedReport_returns404_whenTokenUnknown() throws Exception {
                when(auditService.getSharedReport("unknown-token"))
                                .thenThrow(new ApiException("Shared report not found", HttpStatus.NOT_FOUND));

                mockMvc.perform(get("/api/v1/shared/{token}", "unknown-token"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("Shared report not found"));
        }
}

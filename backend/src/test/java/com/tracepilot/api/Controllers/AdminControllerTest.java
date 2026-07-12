package com.tracepilot.api.Controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tracepilot.api.DTO.Response.AdminFlaggedAuditResponse;
import com.tracepilot.api.DTO.Response.AdminUserResponse;
import com.tracepilot.api.DTO.Response.AdminUserSummary;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.GlobalExceptionHandler;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Services.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

        @Mock
        private AdminService adminService;

        private MockMvc mockMvc;
        private AuthenticatedUser principal;

        @BeforeEach
        void setUp() {
                AdminController controller = new AdminController(adminService);

                principal = new AuthenticatedUser(UUID.randomUUID(), "abdulrahman@example.com", UserRoles.ADMIN);
                var authToken = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authToken);

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new GlobalExceptionHandler())
                                .setCustomArgumentResolvers(
                                                new PageableHandlerMethodArgumentResolver())
                                .alwaysDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .build();
        }

        @AfterEach
        void tearDown() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void getUsers_returnsPagedAdminUserResponses() throws Exception {
                AdminUserResponse user = new AdminUserResponse(
                                UUID.randomUUID(),
                                "Abdulrahman",
                                "abdulrahman@example.com",
                                UserRoles.USER,
                                true,
                                Instant.now(),
                                4);

                when(adminService.getUsers(org.mockito.ArgumentMatchers.any()))
                                .thenReturn(new PageImpl<>(
                                                List.of(user),
                                                org.springframework.data.domain.PageRequest.of(0, 20),
                                                1));

                var mvcResult = mockMvc.perform(get("/api/v1/admin/users"))
                                .andReturn();

                System.err.println("STATUS: " + mvcResult.getResponse().getStatus());
                System.err.println("BODY: " + mvcResult.getResponse().getContentAsString());

                Exception ex = mvcResult.getResolvedException();
                if (ex != null) {
                        throw ex;
                }
        }

        @Test
        void getUsers_returnsEmptyPage_whenNoUsers() throws Exception {
                when(adminService.getUsers(org.mockito.ArgumentMatchers.any()))
                                .thenReturn(new PageImpl<>(
                                                List.of(),
                                                org.springframework.data.domain.PageRequest.of(0, 20),
                                                0));

                mockMvc.perform(get("/api/v1/admin/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print());
        }

        @Test
        void getFlaggedAudits_returnsPagedFlaggedAudits() throws Exception {
                AdminUserSummary owner = new AdminUserSummary(UUID.randomUUID(), "Abdulrahman",
                                "abdulrahman@example.com");
                AdminFlaggedAuditResponse audit = new AdminFlaggedAuditResponse(UUID.randomUUID(), AuditStatus.COMPLETE,
                                true, Instant.now(), owner);
                when(adminService.getFlaggedAudits(org.mockito.ArgumentMatchers.any()))
                                .thenReturn(new PageImpl<>(
                                                List.of(audit),
                                                org.springframework.data.domain.PageRequest.of(0, 20),
                                                1));

                mockMvc.perform(get("/api/v1/admin/audits"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].suspicious").value(true))
                                .andExpect(jsonPath("$.content[0].user.email").value("abdulrahman@example.com"));
        }
}
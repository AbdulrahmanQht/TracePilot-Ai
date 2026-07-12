package com.tracepilot.api.Controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.DTO.Request.UpdateUserRequest;
import com.tracepilot.api.DTO.Response.UserProfileResponse;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Exceptions.GlobalExceptionHandler;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Services.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private AuthenticatedUser principal;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
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

    private UserProfileResponse sampleProfile() {
        return new UserProfileResponse(principal.id(), "Abdulrahman", "abdulrahman@example.com",
                        UserRoles.USER, true, Instant.now(), 1);
    }

    @Test
    void getCurrentUser_returnsProfile() throws Exception {
        when(userService.getCurrentUser(eq(principal))).thenReturn(sampleProfile());

        mockMvc.perform(get("/api/v1/users/me"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value("abdulrahman@example.com"));
    }

    @Test
    void getCurrentUser_returns404_whenUserMissing() throws Exception {
        when(userService.getCurrentUser(eq(principal)))
                        .thenThrow(new ApiException("User not found.", org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/me"))
                        .andExpect(status().isNotFound());
    }

    @Test
    void updateCurrentUser_returnsUpdatedProfile() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("New Name");
        when(userService.updateCurrentUser(eq(principal), any())).thenReturn(sampleProfile());

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value("abdulrahman@example.com"));
    }

    @Test
    void updateCurrentUser_returns400_whenDisplayNameTooShort() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("ab"); // below @Size(min = 3)

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCurrentUser_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                        .andExpect(status().isNoContent());

        verify(userService).deleteCurrentUser(principal);
    }

    @Test
    void deleteCurrentUser_returns404_whenUserMissing() throws Exception {
        org.mockito.Mockito.doThrow(new ApiException("User not found.", org.springframework.http.HttpStatus.NOT_FOUND))
                        .when(userService).deleteCurrentUser(principal);

        mockMvc.perform(delete("/api/v1/users/me"))
                        .andExpect(status().isNotFound());
    }
}

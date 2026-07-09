package com.tracepilot.api.Controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;


import com.tracepilot.api.DTO.Response.UserProfileResponse;
import com.tracepilot.api.DTO.Response.AdminFlaggedAuditResponse;
import com.tracepilot.api.DTO.Response.AdminUserResponse;
import com.tracepilot.api.DTO.Request.UpdateUserRequest;
import com.tracepilot.api.Services.UserService;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Exceptions.ApiException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        return ResponseEntity.ok(userService.getCurrentUser(principal));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateUserRequest request) {

        return ResponseEntity.ok(userService.updateCurrentUser(principal, request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser principal) {

        userService.deleteCurrentUser(principal);

        return ResponseEntity.noContent().build();
    }
}

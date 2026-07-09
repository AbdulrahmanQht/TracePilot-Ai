package com.tracepilot.api.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.UUID;
import com.tracepilot.api.DTO.Request.AuditRequest;
import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Services.AuditService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditResponse> getAudit(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(auditService.getAuditById(id, principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAudit(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        auditService.deleteAudit(id, principal);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<AuditResponse>> listAudits(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.listAudits(principal, pageable));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<AuditResponse> shareAudit(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(auditService.createShareLink(id, principal));
    }

    @DeleteMapping("/{id}/share")
    public ResponseEntity<Void> revokeShareLink(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        auditService.revokeShareLink(id, principal);

        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<AuditResponse> submitAudit(
            @Valid @RequestBody AuditRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        log.info("Received trace audit submission from user ID: {}", principal.id());

        try {
            return ResponseEntity.ok(auditService.initiateAudit(request, principal));
        } catch (DataIntegrityViolationException e) {
            log.info("Race lost on duplicate trace submission for user {}", principal.id());
            return ResponseEntity.ok(auditService.getExistingByHash(request, principal));
        }
    }

}
package com.tracepilot.api.Controllers;

import com.tracepilot.api.DTO.Request.AuditRequest;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Services.AuditService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<TraceAudit> submitAudit(
            @Valid @RequestBody AuditRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {

        log.info("Received trace audit submission from user ID: {}", principal.id());

        TraceAudit createdAudit = auditService.initiateAudit(request, principal);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(createdAudit);
    }
}
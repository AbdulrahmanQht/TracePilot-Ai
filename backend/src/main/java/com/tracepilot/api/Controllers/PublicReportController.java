package com.tracepilot.api.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.Services.AuditService;

@RestController
@RequestMapping("/api/v1/shared")
public class PublicReportController {

    private final AuditService auditService;

    public PublicReportController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<AuditResponse> getSharedReport(@PathVariable String token) {
        return ResponseEntity.ok(auditService.getSharedReport(token));
    }
}

package com.tracepilot.api.Controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tracepilot.api.DTO.Response.ReliabilityResponse;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Services.AuditService;

@RestController
@RequestMapping("/api/v1/reliability")
public class ReliabilityController {

    private final AuditService auditService;

    public ReliabilityController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<ReliabilityResponse>> getReliabilityTrend(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam String repoName,
            @RequestParam String agentTool,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditService.getReliabilityTrend(principal, repoName, agentTool, limit));
    }
}
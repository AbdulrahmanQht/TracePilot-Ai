package com.tracepilot.api.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.tracepilot.api.Enums.AuditInputSource;

public record AuditRequest(
        @NotBlank(message = "Raw trace cannot be empty")
        @Size(max = 80000, message = "Trace exceeds 80,000 characters")
        String rawTrace,
        
        @Size(max = 150)
        String title,
        
        @Size(max = 150)
        String repoName,
        
        @Size(max = 30)
        String agentTool,
        
        AuditInputSource inputSource
) {}
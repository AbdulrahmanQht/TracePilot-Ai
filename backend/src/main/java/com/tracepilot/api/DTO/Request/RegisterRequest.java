package com.tracepilot.api.DTO.Request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String password,

    @Size(max = 100, message = "Display name cannot exceed 100 characters")
    String displayName
){}

package com.tracepilot.api.DTO.Request;

import jakarta.validation.constraints.*;

public record LoginRequest(

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password cannot exceed 100 characters")
    String password
){}

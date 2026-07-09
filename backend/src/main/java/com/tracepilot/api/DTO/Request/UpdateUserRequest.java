package com.tracepilot.api.DTO.Request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(min = 3, max = 50, message = "Display name must be between 3 and 50 characters.") 
        String displayName) {}
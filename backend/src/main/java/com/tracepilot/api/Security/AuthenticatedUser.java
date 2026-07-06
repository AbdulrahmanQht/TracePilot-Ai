package com.tracepilot.api.Security;

import com.tracepilot.api.Enums.UserRoles;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        UserRoles role) {
}
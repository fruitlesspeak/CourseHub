package com.example.backend.auth;

import com.example.backend.user.UserRole;

public record AuthSessionResponse(
        Long userId,
        String name,
        String email,
        UserRole role,
        String dashboardPath
) {
}

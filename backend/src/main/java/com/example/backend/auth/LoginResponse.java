package com.example.backend.auth;

import com.example.backend.user.UserRole;

public record LoginResponse(
        Long userId,
        UserRole role,
        String dashboardPath
) {
}

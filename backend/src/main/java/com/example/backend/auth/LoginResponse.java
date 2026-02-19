package com.example.backend.auth;

public record LoginResponse(
        Long userId,
        UserRole role,
        String dashboardPath
) {
}

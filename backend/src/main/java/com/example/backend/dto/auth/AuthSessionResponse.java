package com.example.backend.dto.auth;

import com.example.backend.entity.UserRole;

public record AuthSessionResponse(
        Integer userId,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        String dashboardPath
) {
}

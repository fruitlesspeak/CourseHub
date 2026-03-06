package com.example.backend.dto.auth;

import com.example.backend.entity.UserRole;

public record LoginResponse(
        Integer userId,
        UserRole role,
        String dashboardPath
) {
}

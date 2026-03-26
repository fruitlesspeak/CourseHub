package com.example.backend.dto.auth;

import com.example.backend.entity.UserRole;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull UserRole role
) {}

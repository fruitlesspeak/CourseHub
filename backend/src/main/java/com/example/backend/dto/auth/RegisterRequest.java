package com.example.backend.dto.auth;

import com.example.backend.entity.UserRole;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "First name is required.")
        @Size(max = 100, message = "First name must be 100 characters or fewer.")
        String firstName,
        @NotBlank(message = "Last name is required.")
        @Size(max = 100, message = "Last name must be 100 characters or fewer.")
        String lastName,
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 320, message = "Email must be 320 characters or fewer.")
        String email,
        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        String password,
        @NotNull(message = "Role is required.")
        UserRole role
) {}

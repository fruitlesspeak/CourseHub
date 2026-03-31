package com.example.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 320, message = "Email must be 320 characters or fewer.")
        String email,
        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 200, message = "Password must be between 8 and 200 characters.")
        String password
) {
}

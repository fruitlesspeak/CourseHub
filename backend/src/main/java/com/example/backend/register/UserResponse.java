package com.example.backend.register;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}

package com.example.backend.dto.auth;

import com.example.backend.entity.User;
import com.example.backend.entity.UserRole;

public record UserResponse(
        Integer id,
        String firstName,
        String lastName,
        String email,
        UserRole role
) {
    public static UserResponse from(User u) {
        String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
        String last = u.getLastName() == null ? "" : u.getLastName().trim();
        UserRole role = u.isProfessor() ? UserRole.PROFESSOR : UserRole.STUDENT;
        return new UserResponse(u.getId(), first, last, u.getEmail(), role);
    }
}

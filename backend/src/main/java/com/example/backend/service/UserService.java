package com.example.backend.service;

import com.example.backend.dto.UserDto;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository        userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository  = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public UserDto.Response create(UserDto.CreateRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + req.getEmail());
        }
        validateRoleConsistency(req.isProfessor(), req.getStudentId(), req.getProfessorId());

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .isProfessor(req.isProfessor())
                .professorId(req.getProfessorId())
                .studentId(req.getStudentId())
                .build();

        return toResponse(userRepository.save(user));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserDto.Response> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserDto.Response findByUuid(UUID uuid) {
        return toResponse(getByUuid(uuid));
    }

    @Transactional(readOnly = true)
    public List<UserDto.Response> findProfessors() {
        return userRepository.findByIsProfessor(true).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto.Response> findStudents() {
        return userRepository.findByIsProfessor(false).stream().map(this::toResponse).toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public UserDto.Response update(UUID uuid, UserDto.UpdateRequest req) {
        User user = getByUuid(uuid);

        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + req.getEmail());
            }
            user.setEmail(req.getEmail());
        }
        if (req.getFirstName()   != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()    != null) user.setLastName(req.getLastName());
        if (req.getIsProfessor() != null) user.setProfessor(req.getIsProfessor());
        if (req.getProfessorId() != null) user.setProfessorId(req.getProfessorId());
        if (req.getStudentId()   != null) user.setStudentId(req.getStudentId());

        validateRoleConsistency(user.isProfessor(), user.getStudentId(), user.getProfessorId());

        return toResponse(userRepository.save(user));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(UUID uuid) {
        userRepository.delete(getByUuid(uuid));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User getByUuid(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + uuid));
    }

    private void validateRoleConsistency(boolean isProfessor, String studentId, Integer professorId) {
        if (isProfessor && studentId != null) {
            throw new IllegalArgumentException("A professor cannot have a student_id.");
        }
    }

    private UserDto.Response toResponse(User u) {
        return UserDto.Response.builder()
                .id(u.getId())
                .uuid(u.getUuid())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .isProfessor(u.isProfessor())
                .professorId(u.getProfessorId())
                .studentId(u.getStudentId())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
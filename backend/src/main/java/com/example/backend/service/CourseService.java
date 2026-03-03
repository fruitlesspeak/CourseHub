package com.example.backend.service;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.Course;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository   userRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository   = userRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public CourseDto.Response create(CourseDto.CreateRequest req, Integer professorId) {
        ensureProfessorExists(professorId);

        Course course = Course.builder()
                .title(req.getTitle())
                .code(req.getCode())
                .description(req.getDescription())
                .link(normalizeCourseLink(req.getLink()))
                .professorId(professorId)
                .build();

        return toResponse(courseRepository.save(course));
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CourseDto.Response> findAll() {
        return courseRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CourseDto.Response findByUuid(UUID uuid) {
        return toResponse(getByUuid(uuid));
    }

    @Transactional(readOnly = true)
    public List<CourseDto.Response> findByProfessor(Integer professorId) {
        return courseRepository.findByProfessorId(professorId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseDto.Response> search(String title) {
        return courseRepository.findByTitleContainingIgnoreCase(title)
                .stream().map(this::toResponse).toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public CourseDto.Response update(UUID uuid, CourseDto.UpdateRequest req) {
        Course course = getByUuid(uuid);

        if (req.getTitle()       != null) course.setTitle(req.getTitle());
        if (req.getCode()        != null) course.setCode(req.getCode());
        if (req.getDescription() != null) course.setDescription(req.getDescription());
        if (req.getLink()        != null) course.setLink(normalizeCourseLink(req.getLink()));
        if (req.getProfessorId() != null) {
            ensureProfessorExists(req.getProfessorId());
            course.setProfessorId(req.getProfessorId());
        }

        return toResponse(courseRepository.save(course));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(UUID uuid) {
        courseRepository.delete(getByUuid(uuid));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Course getByUuid(UUID uuid) {
        return courseRepository.findByUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + uuid));
    }

    private void ensureProfessorExists(Integer professorId) {
        boolean valid = userRepository.findById(professorId)
                .map(u -> u.isProfessor())
                .orElse(false);
        if (!valid) {
            throw new IllegalArgumentException("No professor found with id: " + professorId);
        }
    }

    private static String normalizeCourseLink(String rawLink) {
        if (rawLink == null) {
            return null;
        }

        String trimmed = rawLink.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String normalized = trimmed.startsWith("www.") ? "https://" + trimmed : trimmed;
        if (!normalized.startsWith("https://")) {
            throw new IllegalArgumentException("Course link must start with https:// or www.");
        }

        try {
            URI uri = new URI(normalized);
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Course link is invalid.");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Course link is invalid.");
        }

        return normalized;
    }

    private CourseDto.Response toResponse(Course c) {
        return CourseDto.Response.builder()
                .id(c.getId())
                .uuid(c.getUuid())
                .title(c.getTitle())
                .code(c.getCode())
                .description(c.getDescription())
                .link(c.getLink())
                .professorId(c.getProfessorId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

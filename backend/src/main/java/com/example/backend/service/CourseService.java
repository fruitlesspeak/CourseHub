package com.example.backend.service;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.Course;
import com.example.backend.exception.CourseAccessDeniedException;
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
                .description(normalizeOptionalText(req.getDescription()))
                .link(normalizeCourseLink(req.getLink()))
                .tags(normalizeOptionalText(req.getTags()))
                .material(normalizeOptionalText(req.getMaterial()))
                .dueDate(req.getDueDate())
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

    public CourseDto.Response update(UUID uuid, CourseDto.UpdateRequest req, Integer requestingProfessorId) {
        Course course = getByUuid(uuid);
        ensureProfessorOwnsCourse(course, requestingProfessorId);

        if (req.getTitle()       != null) course.setTitle(req.getTitle());
        if (req.getCode()        != null) course.setCode(req.getCode());
        if (req.getDescription() != null) course.setDescription(normalizeOptionalText(req.getDescription()));
        if (req.getLink()        != null) course.setLink(normalizeCourseLink(req.getLink()));
        if (req.getTags()        != null) course.setTags(normalizeOptionalText(req.getTags()));
        if (req.getMaterial()    != null) course.setMaterial(normalizeOptionalText(req.getMaterial()));
        if (req.getDueDate()     != null) course.setDueDate(req.getDueDate());

        return toResponse(courseRepository.save(course));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(UUID uuid, Integer requestingProfessorId) {
        Course course = getByUuid(uuid);
        ensureProfessorOwnsCourse(course, requestingProfessorId);
        courseRepository.delete(course);
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

    private static void ensureProfessorOwnsCourse(Course course, Integer requestingProfessorId) {
        if (!course.getProfessorId().equals(requestingProfessorId)) {
            throw new CourseAccessDeniedException("You can only modify your own courses.");
        }
    }

    private static String normalizeOptionalText(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
                .tags(c.getTags())
                .material(c.getMaterial())
                .dueDate(c.getDueDate())
                .professorId(c.getProfessorId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

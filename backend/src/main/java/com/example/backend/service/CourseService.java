package com.example.backend.service;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.Course;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CourseDto.Response create(CourseDto.CreateRequest req) {
        ensureProfessorExists(req.getProfessorId());

        Course course = Course.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .professorId(req.getProfessorId())
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
        if (req.getDescription() != null) course.setDescription(req.getDescription());
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

    private CourseDto.Response toResponse(Course c) {
        return CourseDto.Response.builder()
                .id(c.getId())
                .uuid(c.getUuid())
                .title(c.getTitle())
                .description(c.getDescription())
                .professorId(c.getProfessorId())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
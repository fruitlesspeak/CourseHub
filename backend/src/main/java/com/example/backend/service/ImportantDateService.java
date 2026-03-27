package com.example.backend.service;

import com.example.backend.dto.ImportantDateDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.ImportantDate;
import com.example.backend.entity.User;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.ImportantDateRepository;
import com.example.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class ImportantDateService {

    private final ImportantDateRepository importantDateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public ImportantDateService(ImportantDateRepository importantDateRepository,
                                CourseRepository courseRepository,
                                UserRepository userRepository) {
        this.importantDateRepository = importantDateRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    // -- Create ----------------------------------------------------------------

    public ImportantDateDto.Response create(Integer courseId,
                                            ImportantDateDto.CreateRequest req,
                                            Integer professorId) {
        Course course = getCourse(courseId);
        ensureProfessorOwnsCourse(course, professorId);
        ensureProfessorExists(professorId);

        ImportantDate importantDate = ImportantDate.builder()
                .courseId(courseId)
                .createdByUserId(professorId)
                .title(req.getTitle())
                .description(normalizeOptionalText(req.getDescription()))
                .dueAt(req.getDueAt())
                .build();

        return toResponse(importantDateRepository.save(importantDate));
    }

    // -- Read ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ImportantDateDto.Response> findByCourse(Integer courseId) {
        ensureCourseExists(courseId);
        return importantDateRepository.findByCourseIdOrderByDueAtAsc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportantDateDto.Response findById(Integer id) {
        return toResponse(getById(id));
    }

    @Transactional(readOnly = true)
    public List<ImportantDateDto.Response> findByCourseInDateRange(List<Integer> courseIds,
                                                                   OffsetDateTime from,
                                                                   OffsetDateTime to) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        return importantDateRepository.findByCourseIdInAndDueAtBetween(courseIds, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -- Update ----------------------------------------------------------------

    public ImportantDateDto.Response update(Integer id,
                                            ImportantDateDto.UpdateRequest req,
                                            Integer professorId) {
        ImportantDate importantDate = getById(id);
        Course course = getCourse(importantDate.getCourseId());
        ensureProfessorOwnsCourse(course, professorId);

        if (req.getTitle() != null) {
            importantDate.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            importantDate.setDescription(normalizeOptionalText(req.getDescription()));
        }
        if (req.getDueAt() != null) {
            importantDate.setDueAt(req.getDueAt());
        }

        return toResponse(importantDateRepository.save(importantDate));
    }

    // -- Delete ----------------------------------------------------------------

    public void delete(Integer id, Integer professorId) {
        ImportantDate importantDate = getById(id);
        Course course = getCourse(importantDate.getCourseId());
        ensureProfessorOwnsCourse(course, professorId);
        importantDateRepository.delete(importantDate);
    }

    // -- Helpers ---------------------------------------------------------------

    private ImportantDate getById(Integer id) {
        return importantDateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Important date not found: " + id));
    }

    private Course getCourse(Integer courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + courseId));
    }

    private void ensureCourseExists(Integer courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course not found: " + courseId);
        }
    }

    private void ensureProfessorExists(Integer professorId) {
        User user = userRepository.findById(professorId)
                .orElseThrow(() -> new IllegalArgumentException("No professor found with id: " + professorId));

        if (!user.isProfessor()) {
            throw new IllegalArgumentException("No professor found with id: " + professorId);
        }
    }

    private static void ensureProfessorOwnsCourse(Course course, Integer requestingProfessorId) {
        if (!course.getProfessorId().equals(requestingProfessorId)) {
            throw new CourseAccessDeniedException("You can only manage important dates for your own courses.");
        }
    }

    private static String normalizeOptionalText(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ImportantDateDto.Response toResponse(ImportantDate importantDate) {
        return ImportantDateDto.Response.builder()
                .id(importantDate.getId())
                .courseId(importantDate.getCourseId())
                .createdByUserId(importantDate.getCreatedByUserId())
                .title(importantDate.getTitle())
                .description(importantDate.getDescription())
                .dueAt(importantDate.getDueAt())
                .createdAt(importantDate.getCreatedAt())
                .updatedAt(importantDate.getUpdatedAt())
                .build();
    }
}

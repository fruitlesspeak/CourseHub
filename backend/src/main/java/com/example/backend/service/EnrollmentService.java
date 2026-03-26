package com.example.backend.service;

import org.springframework.stereotype.Service;

import com.example.backend.entity.Enrollment;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.EnrollmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, CourseRepository courseRepository, UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void enrollOrReactivate(Integer studentId, Integer courseId) {
        ensureStudentExists(studentId);

        Optional<Enrollment> existing = enrollmentRepository
                .findByUserIdAndCourseId(studentId, courseId);

        if (existing.isPresent()) {
            Enrollment e = existing.get();

            if (Boolean.TRUE.equals(e.getIsActive())) {
                throw new IllegalStateException("User already enrolled in this course");
            }

            // Reactivate
            e.setIsActive(true);
            enrollmentRepository.save(e);
            return;
        }

        // Create new enrollment
        Enrollment e = new Enrollment();
        e.setUserId(studentId);
        e.setCourseId(courseId);
        e.setIsActive(true);

        enrollmentRepository.save(e);
    }

    @Transactional
    public void deactivate(Integer studentId, Integer courseId) {
        ensureStudentExists(studentId);

        Enrollment e = enrollmentRepository
                .findByUserIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found"));

        if (!Boolean.TRUE.equals(e.getIsActive())) {
            throw new IllegalStateException("Enrollment already inactive");
        }

        e.setIsActive(false);
        enrollmentRepository.save(e);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Enrollment> findByUser(Integer userId) {
        return enrollmentRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByCourse(Integer courseId) {
        return enrollmentRepository.findByCourseIdAndIsActiveTrue(courseId);
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private void ensureStudentExists(Integer studentId) {
        boolean valid = userRepository.findById(studentId)
                .map(u -> !u.isProfessor())
                .orElse(false);
        if (!valid) {
            throw new IllegalArgumentException("No student found with id: " + studentId);
        }
    }
}

package com.example.backend.service;

import org.springframework.stereotype.Service;

import com.example.backend.entity.Enrollment;
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
    public void enrollOrReactivate(Integer userId, Integer courseId) {
        Optional<Enrollment> existing = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId);

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
        e.setUserId(userId);
        e.setCourseId(courseId);
        e.setIsActive(true);

        enrollmentRepository.save(e);
    }

    @Transactional
    public void deactivate(Integer userId, Integer courseId) {
        Enrollment e = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found"));

        if (!Boolean.TRUE.equals(e.getIsActive())) {
            throw new IllegalStateException("Enrollment already inactive");
        }

        e.setIsActive(false);
        enrollmentRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getActiveEnrollmentsByUser(Integer userId) {
        return enrollmentRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getActiveEnrollmentsByCourse(Integer courseId) {
        return enrollmentRepository.findByCourseIdAndIsActiveTrue(courseId);
    }


    private void ensureUserExists(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found");
        }
    }

    private void ensureCourseExists(Integer courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course not found");
        }
    }
}

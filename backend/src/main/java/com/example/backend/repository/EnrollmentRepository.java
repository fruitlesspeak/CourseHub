package com.example.backend.repository;

import com.example.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    
    Optional<Enrollment> findByUserIdAndCourseId(Integer userId, Integer courseId);
    List<Enrollment> findByUserIdAndIsActiveTrue(Integer userId);
    List<Enrollment> findByCourseIdAndIsActiveTrue(Integer courseId);
}

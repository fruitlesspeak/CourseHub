package com.example.backend.repository;

import com.example.backend.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    
    Optional<Enrollment> findByUserIdAndCourseId(Integer userId, Integer courseId);
    List<Enrollment> findByUserIdAndIsActiveTrue(Integer userId);
    List<Enrollment> findByCourseIdAndIsActiveTrue(Integer courseId);
    boolean existsByUserIdAndCourseIdAndIsActiveTrue(Integer userId, Integer courseId);
}

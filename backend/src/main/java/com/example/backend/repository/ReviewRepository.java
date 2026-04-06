package com.example.backend.repository;

import com.example.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    boolean existsByUserIdAndCourseId(Integer userId, Integer courseId);

    List<Review> findByCourseIdOrderByCreatedAtDesc(Integer courseId);

    List<Review> findByCourseIdInOrderByCreatedAtDesc(List<Integer> courseIds);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.courseId = :courseId")
    Double findAvgRatingByCourseId(@Param("courseId") Integer courseId);

    long countByCourseId(Integer courseId);
}

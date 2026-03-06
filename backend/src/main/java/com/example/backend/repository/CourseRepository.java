package com.example.backend.repository;

import com.example.backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    Optional<Course> findByUuid(UUID uuid);
    List<Course>     findByProfessorId(Integer professorId);
    List<Course>     findByTitleContainingIgnoreCase(String title);
}
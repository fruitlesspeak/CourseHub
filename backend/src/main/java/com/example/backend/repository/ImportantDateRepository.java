package com.example.backend.repository;

import com.example.backend.entity.ImportantDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ImportantDateRepository extends JpaRepository<ImportantDate, Integer> {

    List<ImportantDate> findByCourseId(Integer courseId);
    List<ImportantDate> findByCourseIdOrderByDueAtAsc(Integer courseId);
    List<ImportantDate> findByCourseIdInAndDueAtBetween(List<Integer> courseIds,
                                                         OffsetDateTime from,
                                                         OffsetDateTime to);
}

package com.example.backend.service;

import com.example.backend.dto.ReviewDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private final ReviewRepository     reviewRepository;
    private final CourseRepository     courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository       userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         CourseRepository courseRepository,
                         EnrollmentRepository enrollmentRepository,
                         UserRepository userRepository) {
        this.reviewRepository     = reviewRepository;
        this.courseRepository     = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository       = userRepository;
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    public ReviewDto.Response submit(ReviewDto.CreateRequest req, Integer studentId, UUID courseUuid) {
        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));

        boolean wasEnrolled = enrollmentRepository
                .findByUserIdAndCourseId(studentId, course.getId())
                .isPresent();
        if (!wasEnrolled) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You must be enrolled in a course to leave a review.");
        }

        if (reviewRepository.existsByUserIdAndCourseId(studentId, course.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You have already reviewed this course.");
        }

        Review review = new Review();
        review.setUserId(studentId);
        review.setCourseId(course.getId());
        review.setRating(req.getRating());
        review.setComment(req.getComment());

        return toResponse(reviewRepository.save(review), course.getTitle());
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReviewDto.Response> findByCourse(UUID courseUuid) {
        Course course = courseRepository.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));

        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(course.getId())
                .stream()
                .map(r -> toResponse(r, course.getTitle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDto.Response> findByProfessor(Integer professorId) {
        List<Course> courses = courseRepository.findByProfessorId(professorId);
        if (courses.isEmpty()) return List.of();

        List<Integer> courseIds = courses.stream().map(Course::getId).toList();
        Map<Integer, String> titleById = courses.stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle));

        return reviewRepository.findByCourseIdInOrderByCreatedAtDesc(courseIds)
                .stream()
                .map(r -> toResponse(r, titleById.get(r.getCourseId())))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ReviewDto.Response toResponse(Review r, String courseTitle) {
        User reviewer = userRepository.findById(r.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Reviewer not found."));

        return ReviewDto.Response.builder()
                .id(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .reviewerFirstName(reviewer.getFirstName())
                .reviewerLastName(reviewer.getLastName())
                .courseId(r.getCourseId())
                .courseTitle(courseTitle)
                .createdAt(r.getCreatedAt())
                .build();
    }
}

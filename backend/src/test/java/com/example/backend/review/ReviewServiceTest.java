package com.example.backend.review;

import com.example.backend.dto.ReviewDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.Review;
import com.example.backend.entity.User;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final UUID   COURSE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int    COURSE_ID   = 10;
    private static final int    STUDENT_ID  = 1;
    private static final int    PROF_ID     = 7;

    @Mock private ReviewRepository     reviewRepository;
    @Mock private CourseRepository     courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private UserRepository       userRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, courseRepository, enrollmentRepository, userRepository);
    }

    // ── submit ────────────────────────────────────────────────────────────────

    @Test
    void mutation_submitSavesReviewWithCorrectFields() {
        stubCourse();
        stubEnrolled();
        when(reviewRepository.existsByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        stubSave();
        stubReviewer();

        ReviewDto.CreateRequest req = new ReviewDto.CreateRequest();
        req.setRating(4);
        req.setComment("Great course");

        reviewService.submit(req, STUDENT_ID, COURSE_UUID);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();

        assertEquals(STUDENT_ID, saved.getUserId());
        assertEquals(COURSE_ID,  saved.getCourseId());
        assertEquals(4,          saved.getRating());
        assertEquals("Great course", saved.getComment());
    }

    @Test
    void mutation_submitReturnsMappedResponseWithReviewerName() {
        stubCourse();
        stubEnrolled();
        when(reviewRepository.existsByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        stubSave();
        stubReviewer();

        ReviewDto.CreateRequest req = new ReviewDto.CreateRequest();
        req.setRating(5);
        req.setComment("Excellent");

        ReviewDto.Response response = reviewService.submit(req, STUDENT_ID, COURSE_UUID);

        assertEquals(5,           response.getRating());
        assertEquals("Excellent", response.getComment());
        assertEquals("Sam",       response.getReviewerFirstName());
        assertEquals("Student",   response.getReviewerLastName());
        assertEquals(COURSE_ID,   response.getCourseId());
        assertEquals("Databases", response.getCourseTitle());
    }

    @Test
    void mutation_submitWithNullCommentSavesNullComment() {
        stubCourse();
        stubEnrolled();
        when(reviewRepository.existsByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        stubSave();
        stubReviewer();

        ReviewDto.CreateRequest req = new ReviewDto.CreateRequest();
        req.setRating(3);
        req.setComment(null);

        reviewService.submit(req, STUDENT_ID, COURSE_UUID);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(null, captor.getValue().getComment());
    }

    @Test
    void mutation_submitWhenCourseNotFoundThrowsEntityNotFound() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> reviewService.submit(new ReviewDto.CreateRequest(), STUDENT_ID, COURSE_UUID));

        assertEquals("Course not found.", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void mutation_submitWhenStudentNotEnrolledThrowsForbidden() {
        stubCourse();
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.submit(new ReviewDto.CreateRequest(), STUDENT_ID, COURSE_UUID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void mutation_submitWhenDroppedStudentIsStillAllowedToReview() {
        stubCourse();
        // dropped enrollment (isActive = false) — still present in the table
        Enrollment dropped = new Enrollment();
        dropped.setUserId(STUDENT_ID);
        dropped.setCourseId(COURSE_ID);
        dropped.setIsActive(false);
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(dropped));
        when(reviewRepository.existsByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        stubSave();
        stubReviewer();

        ReviewDto.CreateRequest req = new ReviewDto.CreateRequest();
        req.setRating(3);

        reviewService.submit(req, STUDENT_ID, COURSE_UUID);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void mutation_submitWhenAlreadyReviewedThrowsConflict() {
        stubCourse();
        stubEnrolled();
        when(reviewRepository.existsByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reviewService.submit(new ReviewDto.CreateRequest(), STUDENT_ID, COURSE_UUID));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
    }

    // ── findByCourse ──────────────────────────────────────────────────────────

    @Test
    void mutation_findByCourseReturnsReviewsMappedWithReviewerName() {
        stubCourse();
        Review review = existingReview(1, STUDENT_ID, COURSE_ID, 4, "Nice");
        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(COURSE_ID)).thenReturn(List.of(review));
        stubReviewer();

        List<ReviewDto.Response> result = reviewService.findByCourse(COURSE_UUID);

        assertEquals(1, result.size());
        assertEquals(4,      result.get(0).getRating());
        assertEquals("Nice", result.get(0).getComment());
        assertEquals("Sam",  result.get(0).getReviewerFirstName());
        assertEquals("Databases", result.get(0).getCourseTitle());
    }

    @Test
    void mutation_findByCourseWithNoReviewsReturnsEmptyList() {
        stubCourse();
        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(COURSE_ID)).thenReturn(List.of());

        List<ReviewDto.Response> result = reviewService.findByCourse(COURSE_UUID);

        assertTrue(result.isEmpty());
    }

    @Test
    void mutation_findByCourseWhenCourseNotFoundThrowsEntityNotFound() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> reviewService.findByCourse(COURSE_UUID));

        assertEquals("Course not found.", ex.getMessage());
    }

    // ── findByProfessor ───────────────────────────────────────────────────────

    @Test
    void mutation_findByProfessorReturnsReviewsWithCourseTitle() {
        Course course = buildCourse(COURSE_ID, "Databases");
        when(courseRepository.findByProfessorId(PROF_ID)).thenReturn(List.of(course));
        Review review = existingReview(1, STUDENT_ID, COURSE_ID, 5, "Loved it");
        when(reviewRepository.findByCourseIdInOrderByCreatedAtDesc(List.of(COURSE_ID)))
                .thenReturn(List.of(review));
        stubReviewer();

        List<ReviewDto.Response> result = reviewService.findByProfessor(PROF_ID);

        assertEquals(1,           result.size());
        assertEquals(5,           result.get(0).getRating());
        assertEquals("Loved it",  result.get(0).getComment());
        assertEquals("Databases", result.get(0).getCourseTitle());
        assertEquals(COURSE_ID,   result.get(0).getCourseId());
    }

    @Test
    void mutation_findByProfessorAcrossMultipleCoursesReturnsAllReviews() {
        Course c1 = buildCourse(10, "Databases");
        Course c2 = buildCourse(20, "Algorithms");

        User reviewer2 = buildUser(2, "Jane", "Doe");

        Review r1 = existingReview(1, STUDENT_ID, 10, 4, "Good");
        Review r2 = existingReview(2, 2,          20, 5, "Great");

        when(courseRepository.findByProfessorId(PROF_ID)).thenReturn(List.of(c1, c2));
        when(reviewRepository.findByCourseIdInOrderByCreatedAtDesc(List.of(10, 20)))
                .thenReturn(List.of(r1, r2));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(buildUser(STUDENT_ID, "Sam", "Student")));
        when(userRepository.findById(2)).thenReturn(Optional.of(reviewer2));

        List<ReviewDto.Response> result = reviewService.findByProfessor(PROF_ID);

        assertEquals(2,            result.size());
        assertEquals("Databases",  result.get(0).getCourseTitle());
        assertEquals("Algorithms", result.get(1).getCourseTitle());
    }

    @Test
    void mutation_findByProfessorWithNoCoursesReturnsEmptyList() {
        when(courseRepository.findByProfessorId(PROF_ID)).thenReturn(List.of());

        List<ReviewDto.Response> result = reviewService.findByProfessor(PROF_ID);

        assertTrue(result.isEmpty());
        verify(reviewRepository, never()).findByCourseIdInOrderByCreatedAtDesc(any());
    }

    @Test
    void mutation_findByProfessorWithCoursesButNoReviewsReturnsEmptyList() {
        when(courseRepository.findByProfessorId(PROF_ID)).thenReturn(List.of(buildCourse(COURSE_ID, "Databases")));
        when(reviewRepository.findByCourseIdInOrderByCreatedAtDesc(List.of(COURSE_ID))).thenReturn(List.of());

        List<ReviewDto.Response> result = reviewService.findByProfessor(PROF_ID);

        assertTrue(result.isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stubCourse() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(buildCourse(COURSE_ID, "Databases")));
    }

    private void stubEnrolled() {
        Enrollment e = new Enrollment();
        e.setUserId(STUDENT_ID);
        e.setCourseId(COURSE_ID);
        e.setIsActive(true);
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.of(e));
    }

    private void stubSave() {
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0, Review.class);
            r.setId(1);
            r.setCreatedAt(OffsetDateTime.parse("2026-04-01T10:00:00Z"));
            return r;
        });
    }

    private void stubReviewer() {
        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(buildUser(STUDENT_ID, "Sam", "Student")));
    }

    private static Course buildCourse(int id, String title) {
        Course c = new Course();
        c.setId(id);
        c.setUuid(COURSE_UUID);
        c.setTitle(title);
        return c;
    }

    private static User buildUser(int id, String firstName, String lastName) {
        User u = new User();
        u.setId(id);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        return u;
    }

    private static Review existingReview(int id, int userId, int courseId, int rating, String comment) {
        Review r = new Review();
        r.setId(id);
        r.setUserId(userId);
        r.setCourseId(courseId);
        r.setRating(rating);
        r.setComment(comment);
        r.setCreatedAt(OffsetDateTime.parse("2026-04-01T10:00:00Z"));
        return r;
    }
}

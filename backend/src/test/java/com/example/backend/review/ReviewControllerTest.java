package com.example.backend.review;

import com.example.backend.controller.GlobalExceptionHandler;
import com.example.backend.controller.ReviewController;
import com.example.backend.dto.ReviewDto;
import com.example.backend.service.ReviewService;
import com.example.backend.service.SessionAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private static final UUID COURSE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int  STUDENT_ID  = 5;
    private static final int  PROF_ID     = 7;

    private static final String VALID_PAYLOAD = """
            { "rating": 4, "comment": "Great course" }
            """;

    @Mock
    private ReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReviewController controller = new ReviewController(reviewService, new SessionAuthService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── POST /{uuid}/reviews ──────────────────────────────────────────────────

    @Test
    void submitReviewAsStudentReturns201AndResponseBody() throws Exception {
        MockHttpSession session = studentSession();

        ReviewDto.Response response = buildReviewResponse(4, "Great course");
        when(reviewService.submit(any(ReviewDto.CreateRequest.class), eq(STUDENT_ID), eq(COURSE_UUID)))
                .thenReturn(response);

        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Great course"))
                .andExpect(jsonPath("$.reviewerFirstName").value("Sam"))
                .andExpect(jsonPath("$.courseTitle").value("Databases"));

        verify(reviewService).submit(any(ReviewDto.CreateRequest.class), eq(STUDENT_ID), eq(COURSE_UUID));
    }

    @Test
    void submitReviewAsProfessorReturns403() throws Exception {
        MockHttpSession session = professorSession();

        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only students can submit reviews."));

        verifyNoInteractions(reviewService);
    }

    @Test
    void submitReviewWithoutSessionReturns401() throws Exception {
        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please sign in to continue."));

        verifyNoInteractions(reviewService);
    }

    @Test
    void submitReviewWithMissingRatingReturns422() throws Exception {
        MockHttpSession session = studentSession();

        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "comment": "No rating provided" }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rating").value("Rating is required."));

        verifyNoInteractions(reviewService);
    }

    @Test
    void submitReviewWithRatingBelowOneReturns422() throws Exception {
        MockHttpSession session = studentSession();

        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rating": 0 }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rating").value("Rating must be between 1 and 5."));

        verifyNoInteractions(reviewService);
    }

    @Test
    void submitReviewWithRatingAboveFiveReturns422() throws Exception {
        MockHttpSession session = studentSession();

        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rating": 6 }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rating").value("Rating must be between 1 and 5."));

        verifyNoInteractions(reviewService);
    }

    @Test
    void submitReviewWhenAlreadyReviewedReturns409() throws Exception {
        MockHttpSession session = studentSession();
        when(reviewService.submit(any(), eq(STUDENT_ID), eq(COURSE_UUID)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this course."));

        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("You have already reviewed this course."));
    }

    @Test
    void submitReviewWhenNotEnrolledReturns403() throws Exception {
        MockHttpSession session = studentSession();
        when(reviewService.submit(any(), eq(STUDENT_ID), eq(COURSE_UUID)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You must be enrolled in a course to leave a review."));

        mockMvc.perform(post("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PAYLOAD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You must be enrolled in a course to leave a review."));
    }

    // ── GET /{uuid}/reviews ───────────────────────────────────────────────────

    @Test
    void getReviewsByCourseAsAuthenticatedUserReturns200WithList() throws Exception {
        MockHttpSession session = studentSession();
        when(reviewService.findByCourse(COURSE_UUID)).thenReturn(List.of(buildReviewResponse(4, "Nice")));

        mockMvc.perform(get("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(4))
                .andExpect(jsonPath("$[0].comment").value("Nice"));

        verify(reviewService).findByCourse(COURSE_UUID);
    }

    @Test
    void getReviewsByCourseAsProfessorReturns200() throws Exception {
        MockHttpSession session = professorSession();
        when(reviewService.findByCourse(COURSE_UUID)).thenReturn(List.of());

        mockMvc.perform(get("/api/courses/{uuid}/reviews", COURSE_UUID)
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void getReviewsByCourseWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/courses/{uuid}/reviews", COURSE_UUID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please sign in to continue."));

        verifyNoInteractions(reviewService);
    }

    // ── GET /my-reviews ───────────────────────────────────────────────────────

    @Test
    void getProfessorReviewsAsProfessorReturns200WithList() throws Exception {
        MockHttpSession session = professorSession();
        when(reviewService.findByProfessor(PROF_ID)).thenReturn(List.of(buildReviewResponse(5, "Excellent")));

        mockMvc.perform(get("/api/courses/my-reviews")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].comment").value("Excellent"));

        verify(reviewService).findByProfessor(PROF_ID);
    }

    @Test
    void getProfessorReviewsAsStudentReturns403() throws Exception {
        MockHttpSession session = studentSession();

        mockMvc.perform(get("/api/courses/my-reviews")
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only professors can access this endpoint."));

        verifyNoInteractions(reviewService);
    }

    @Test
    void getProfessorReviewsWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/courses/my-reviews"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please sign in to continue."));

        verifyNoInteractions(reviewService);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static MockHttpSession studentSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   STUDENT_ID);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");
        return session;
    }

    private static MockHttpSession professorSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   PROF_ID);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");
        return session;
    }

    private static ReviewDto.Response buildReviewResponse(int rating, String comment) {
        return ReviewDto.Response.builder()
                .id(1)
                .rating(rating)
                .comment(comment)
                .reviewerFirstName("Sam")
                .reviewerLastName("Student")
                .courseId(10)
                .courseTitle("Databases")
                .createdAt(OffsetDateTime.parse("2026-04-01T10:00:00Z"))
                .build();
    }
}

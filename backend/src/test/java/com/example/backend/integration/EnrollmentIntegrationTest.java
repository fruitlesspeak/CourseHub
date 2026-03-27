package com.example.backend.integration;

import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class EnrollmentIntegrationTest {

    @Autowired private MockMvc             mockMvc;
    @Autowired private UserRepository      userRepository;
    @Autowired private CourseRepository    courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;


    private MockHttpSession registerAndLogin(String email, String password,
                                             String firstName, String lastName,
                                             String role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s",
                         "firstName":"%s","lastName":"%s","role":"%s"}
                        """.formatted(email, password, firstName, lastName, role)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String createCourseAndGetUuid(MockHttpSession professorSession,
                                          String title, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/courses")
                .session(professorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","code":"%s"}
                        """.formatted(title, code)))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.uuid");
    }

    // ── TEST 1: Basic enroll pipeline ─────────────────────────────────────────

    @Test
    void studentCanEnrollInCourseAndSeeItInMyCourses() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll1@test.com", "pass123", "Prof", "One", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Enroll Test", "ENR101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll1@test.com", "pass123", "Student", "One", "STUDENT");

        // Enroll
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession))
                .andExpect(status().isCreated());

        // Verify via my-courses
        mockMvc.perform(get("/api/courses/my-courses").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Enroll Test"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── TEST 2: Enroll persists to DB ─────────────────────────────────────────

    @Test
    void enrollmentIsPersistedInDatabase() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll2@test.com", "pass123", "Prof", "Two", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "DB Persist", "DB101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll2@test.com", "pass123", "Student", "Two", "STUDENT");
        Integer studentId = (Integer) studentSession.getAttribute("AUTH_USER_ID");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession))
                .andExpect(status().isCreated());

        // Resolve course DB id from UUID
        var course = courseRepository.findByUuid(UUID.fromString(courseUuid)).orElseThrow();

        boolean persisted = enrollmentRepository
                .existsByUserIdAndCourseIdAndIsActiveTrue(studentId, course.getId());

        assertTrue(persisted);
    }

    // ── TEST 3: Double-enroll returns conflict ────────────────────────────────

    @Test
    void enrollingTwiceInSameCourseReturnsConflict() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll3@test.com", "pass123", "Prof", "Three", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "No Double", "NDL101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll3@test.com", "pass123", "Student", "Three", "STUDENT");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession))
                .andExpect(status().isCreated());

        // Second enroll on an already-active enrollment
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession))
                .andExpect(status().isConflict());
    }

    // ── TEST 4: Unenroll pipeline ─────────────────────────────────────────────

    @Test
    void studentCanUnenrollAndCourseDisappearsFromMyCourses() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll4@test.com", "pass123", "Prof", "Four", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Unenroll Test", "UN101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll4@test.com", "pass123", "Student", "Four", "STUDENT");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/my-courses").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── TEST 5: Unenroll sets isActive=false in DB ────────────────────────────

    @Test
    void unenrollSetsEnrollmentInactiveInDatabase() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll5@test.com", "pass123", "Prof", "Five", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "DB Deactivate", "DDA101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll5@test.com", "pass123", "Student", "Five", "STUDENT");
        Integer studentId = (Integer) studentSession.getAttribute("AUTH_USER_ID");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isNoContent());

        var course = courseRepository.findByUuid(UUID.fromString(courseUuid)).orElseThrow();

        boolean stillActive = enrollmentRepository
                .existsByUserIdAndCourseIdAndIsActiveTrue(studentId, course.getId());

        assertFalse(stillActive);
    }

    // ── TEST 6: Re-enroll after unenroll succeeds ─────────────────────────────

    @Test
    void studentCanReEnrollAfterUnenrolling() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll6@test.com", "pass123", "Prof", "Six", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Re-Enroll", "RE101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll6@test.com", "pass123", "Student", "Six", "STUDENT");

        // First enroll
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isCreated());

        // Unenroll
        mockMvc.perform(delete("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isNoContent());

        // Re-enroll — should succeed (reactivation path)
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isCreated());

        // Should be visible again in my-courses
        mockMvc.perform(get("/api/courses/my-courses").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── TEST 7: Content gating — enrolled student can access ─────────────────

    @Test
    void enrolledStudentCanAccessCourseContent() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll7@test.com", "pass123", "Prof", "Seven", "PROFESSOR");

        MvcResult createResult = mockMvc.perform(post("/api/courses")
                .session(profSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Gated Course","code":"GATE101",
                         "material":"Secret lecture notes"}
                        """))
                .andExpect(status().isCreated()).andReturn();

        String courseUuid = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.uuid");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll7@test.com", "pass123", "Student", "Seven", "STUDENT");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/courses/{uuid}/content", courseUuid)
                .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.material").value("Secret lecture notes"));
    }

    // ── TEST 8: Content gating — non-enrolled student blocked ────────────────

    @Test
    void nonEnrolledStudentCannotAccessCourseContent() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll8@test.com", "pass123", "Prof", "Eight", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Locked Course", "LCK101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll8@test.com", "pass123", "Student", "Eight", "STUDENT");

        // No enroll — direct content access
        mockMvc.perform(get("/api/courses/{uuid}/content", courseUuid)
                .session(studentSession))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("You must be enrolled to access course materials."));
    }

    // ── TEST 9: Content gating — unenrolled student loses access ─────────────

    @Test
    void studentLosesContentAccessAfterUnenrolling() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll9@test.com", "pass123", "Prof", "Nine", "PROFESSOR");

        MvcResult createResult = mockMvc.perform(post("/api/courses")
                .session(profSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Revoked Course","code":"REV101",
                         "material":"Confidential"}
                        """))
                .andExpect(status().isCreated()).andReturn();

        String courseUuid = JsonPath.read(
                createResult.getResponse().getContentAsString(), "$.uuid");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll9@test.com", "pass123", "Student", "Nine", "STUDENT");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isCreated());

        // Confirm access
        mockMvc.perform(get("/api/courses/{uuid}/content", courseUuid)
                .session(studentSession)).andExpect(status().isOk());

        // Unenroll
        mockMvc.perform(delete("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isNoContent());

        // Access should now be denied
        mockMvc.perform(get("/api/courses/{uuid}/content", courseUuid)
                .session(studentSession))
                .andExpect(status().isForbidden());
    }

    // ── TEST 10: Student count reflects enrollment ────────────────────────────

    @Test
    void courseStudentCountIncreasesAfterEnrollment() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll10@test.com", "pass123", "Prof", "Ten", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Count Test", "CNT101");

        // Before: 0 students
        mockMvc.perform(get("/api/courses/{uuid}", courseUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCount").value(0));

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll10@test.com", "pass123", "Student", "Ten", "STUDENT");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isCreated());

        // After: 1 student
        mockMvc.perform(get("/api/courses/{uuid}", courseUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCount").value(1))
                .andExpect(jsonPath("$.students[0].email").value("student-enroll10@test.com"));
    }

    // ── TEST 11: Student count decreases after unenroll ───────────────────────

    @Test
    void courseStudentCountDecreasesAfterUnenrollment() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll11@test.com", "pass123", "Prof", "Eleven", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Decrement Test", "DEC101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll11@test.com", "pass123", "Student", "Eleven", "STUDENT");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/{uuid}", courseUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCount").value(0))
                .andExpect(jsonPath("$.students").isEmpty());
    }

    // ── TEST 12: Unenroll non-existent enrollment returns 404 ─────────────────

    @Test
    void unenrollingFromCourseNeverEnrolledReturns404() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll12@test.com", "pass123", "Prof", "Twelve", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Ghost Unenroll", "GHO101");

        MockHttpSession studentSession = registerAndLogin(
                "student-enroll12@test.com", "pass123", "Student", "Twelve", "STUDENT");

        // Never enrolled — unenroll should 404
        mockMvc.perform(delete("/api/courses/{uuid}/enroll", courseUuid)
                .session(studentSession))
                .andExpect(status().isNotFound());
    }

    // ── TEST 13: Professor cannot enroll ─────────────────────────────────────

    @Test
    void professorCannotEnrollInAnyCourse() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll13@test.com", "pass123", "Prof", "Thirteen", "PROFESSOR");
        String courseUuid = createCourseAndGetUuid(profSession, "Prof Enroll Guard", "PEG101");

        // Professor tries to enroll in their own course
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseUuid)
                .session(profSession))
                .andExpect(status().isForbidden());
    }

    // ── TEST 14: Multiple students isolation ─────────────────────────────────

    @Test
    void multipleStudentsEnrollAndEachSeesOnlyOwnMyCourses() throws Exception {
        MockHttpSession profSession = registerAndLogin(
                "prof-enroll14@test.com", "pass123", "Prof", "Fourteen", "PROFESSOR");
        String courseAUuid = createCourseAndGetUuid(profSession, "Course A", "CA101");
        String courseBUuid = createCourseAndGetUuid(profSession, "Course B", "CB101");

        MockHttpSession studentASession = registerAndLogin(
                "studentA-enroll14@test.com", "pass123", "StudentA", "Fourteen", "STUDENT");
        MockHttpSession studentBSession = registerAndLogin(
                "studentB-enroll14@test.com", "pass123", "StudentB", "Fourteen", "STUDENT");

        // Student A enrolls in Course A only
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseAUuid)
                .session(studentASession)).andExpect(status().isCreated());

        // Student B enrolls in both
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseAUuid)
                .session(studentBSession)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/courses/{uuid}/enroll", courseBUuid)
                .session(studentBSession)).andExpect(status().isCreated());

        // Student A should only see Course A
        mockMvc.perform(get("/api/courses/my-courses").session(studentASession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Course A"));

        // Student B should see both
        mockMvc.perform(get("/api/courses/my-courses").session(studentBSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}

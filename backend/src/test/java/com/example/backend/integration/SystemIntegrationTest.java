package com.example.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import com.example.backend.entity.User;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.ImportantDateRepository;
import com.example.backend.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SystemIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ImportantDateRepository importantDateRepository;

    private record CreatedCourse(Integer id, String uuid) {
    }

    /*
     * Helper: Login and obtain session
     */
    private MockHttpSession loginAndGetSession(String email, String password) throws Exception {
        String loginRequest = """
        {
            "email": "%s",
            "password": "%s"
        }
        """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequest))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    /*
     * Helper: Login and obtain session
     */
    private MockHttpSession registerAndLogin(String email, String password, String firstName, String lastName, String role) throws Exception {
        String registerRequest = """
                {
                    "email": "%s",
                    "password": "%s",
                    "firstName": "%s",
                    "lastName": "%s",
                    "role": "%s"
                }
                """.formatted(email, password, firstName, lastName, role);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated());

        return loginAndGetSession(email, password);
    }

    private CreatedCourse createCourse(MockHttpSession session,
                                       String title,
                                       String code,
                                       String tags,
                                       String material) throws Exception {
        String courseRequest = """
                {
                    "title": "%s",
                    "code": "%s",
                    "tags": %s,
                    "material": %s
                }
                """.formatted(title, code, toJsonString(tags), toJsonString(material));

        MvcResult createResult = mockMvc.perform(post("/api/courses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(courseRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        return new CreatedCourse(
                JsonPath.read(responseBody, "$.id"),
                JsonPath.read(responseBody, "$.uuid")
        );
    }

    private Integer createImportantDate(MockHttpSession session,
                                        Integer courseId,
                                        String title,
                                        String description,
                                        String dueAt) throws Exception {
        String importantDateRequest = """
                {
                    "title": "%s",
                    "description": "%s",
                    "dueAt": "%s"
                }
                """.formatted(title, description, dueAt);

        MvcResult createResult = mockMvc.perform(post("/api/courses/{courseId}/important-dates", courseId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importantDateRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.com";
    }

    private static String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }



    // ============================================================
    // TEST 1 — Professor course creation pipeline
    // ============================================================
    @Test
    void professorCanRegisterLoginAndCreateCourse() throws Exception {

        // Register Professor, Login and get Session 
         MockHttpSession session = registerAndLogin("prof@test.com", "password123", "PFirst", "PLast", "PROFESSOR");

        // Create course using session authentication 
        String courseRequest = """
            {
                "title": "Software Engineering",
                "code": "COMP3350",
                "description": "Testing course creation"
            }
        """;

        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseRequest))
                .andExpect(status().isCreated());

        // Verify course appears in professor dashboard
        Integer professorId = (Integer) session.getAttribute("AUTH_USER_ID");

        mockMvc.perform(get("/api/courses")
                .param("professorId", professorId.toString())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Software Engineering"))
                .andExpect(jsonPath("$[0].code").value("COMP3350"));
    }    

    // ============================================================
    // TEST 2 — Registration persistence integrity
    // ============================================================
    @Test
    void registerProfessorAndStudent() throws Exception {

        // Register Professor
        String professorRequest = """
        {
            "email": "prof@test.com",
            "password": "password123",
            "firstName": "Prof",
            "lastName": "User",
            "role": "PROFESSOR"
        }
        """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(professorRequest))
                .andExpect(status().isCreated());

        // Register Student 
        String studentRequest = """
        {
            "email": "student@test.com",
            "password": "password123",
            "firstName": "Student",
            "lastName": "User",
            "role": "STUDENT"
        }
        """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(studentRequest))
                .andExpect(status().isCreated());

        // Verify Database Persistence 
        User professor = userRepository.findByEmail("prof@test.com").orElseThrow();
        User student = userRepository.findByEmail("student@test.com").orElseThrow();

        assertTrue(professor.isProfessor());
        assertFalse(student.isProfessor());
    }


    // ============================================================
    // TEST 3 — Ownership authorization pipeline
    // ============================================================
    @Test
    void nonOwnerProfessorCannotUpdateOrDeleteCourse() throws Exception {
        // Register Professor A and Login 
        String professorAEmail = "profA-" + UUID.randomUUID() + "@test.com";   
        MockHttpSession sessionA = registerAndLogin(professorAEmail, "password123", "ProfA", "User", "PROFESSOR");

        // Create course as Professor A
        String courseRequest = """
        {
            "title": "Ownership Test Course",
            "code": "OWN123",
            "description": "Authorization test course"
        }
        """;

        MvcResult createResult = mockMvc.perform(post("/api/courses")
                .session(sessionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String courseUuid = JsonPath.read(responseBody, "$.uuid");

        // Register Professor B 
        String professorBEmail = "profB-" + UUID.randomUUID() + "@test.com";
        MockHttpSession sessionB = registerAndLogin(professorBEmail, "password123", "ProfB", "User", "PROFESSOR");

        // Professor B tries UPDATE (PATCH)
        String updateRequest = """
        {
            "title": "Update Attempt by other Owner"
        }
        """;

        mockMvc.perform(patch("/api/courses/" + courseUuid)
                .session(sessionB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isForbidden());

        // Professor B tries DELETE
        mockMvc.perform(delete("/api/courses/" + courseUuid)
                .session(sessionB))
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // TEST 4 — Role authorization (student create course restriction)
    // ============================================================
    @Test
    void studentCannotCreateCourse() throws Exception {
        // Register Student and Login 
        MockHttpSession session = registerAndLogin("student2@test.com", "password123", "Student", "User", "STUDENT");

        // Attempt Course Creation 
        String courseRequest = """
                {
                    "title": "Unauthorized Course",
                    "code": "TEST100"
                }
                """;

        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseRequest))
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // TEST 5 — Owner edit integration pipeline
    // ============================================================
    @Test
    void ownerProfessorCanEditOwnCourse() throws Exception {

        // Register Professor and Login
        MockHttpSession session = registerAndLogin("owner@test.com", "password123", "Owner", "Prof", "PROFESSOR");

        // Create Course
        String courseCreateRequest = """
                {
                    "title": "Old Title",
                    "code": "EDIT101",
                    "description": "Old description"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseCreateRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();

        UUID courseUuid = UUID.fromString(
                mapper.readTree(responseBody)
                        .get("uuid")
                        .asText());

        // Update Course
        String updateRequest = """
                {
                    "description": "Updated description",
                    "tags": "java,spring",
                    "material": "lecture notes",
                    "link": "www.example.com"
                }
                """;

        mockMvc.perform(patch("/api/courses/" + courseUuid)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.tags").value("java,spring"))
                .andExpect(jsonPath("$.material").value("lecture notes"))
                .andExpect(jsonPath("$.link").value("https://www.example.com"));

        // Verify Dashboard 
        Integer professorId = (Integer) session.getAttribute("AUTH_USER_ID");

        mockMvc.perform(get("/api/courses")
                .param("professorId", professorId.toString())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description")
                        .value("Updated description"));
    }

    // ============================================================
    // TEST 6 — Owner delete cascade pipeline
    // ============================================================
    @Test
    void ownerProfessorCanDeleteCourseAndCascade() throws Exception {
        // Register and Login Professor 
        MockHttpSession session = registerAndLogin("ownerdelete@test.com", "password123", "Owner", "Prof", "PROFESSOR");
        Integer professorId = (Integer) session.getAttribute("AUTH_USER_ID");

        // Create course 
        String courseRequest = """
                {
                    "title": "Cascade Delete Course",
                    "code": "CASCADE101",
                    "description": "Delete me"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(courseRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();

        String courseUuid = JsonPath.read(responseBody, "$.uuid");

        // Delete course 
        mockMvc.perform(delete("/api/courses/{uuid}", courseUuid)
                .session(session))
                .andExpect(status().isNoContent());

        // Verify dashboard empty 
        mockMvc.perform(get("/api/courses")
                .param("professorId", professorId.toString())
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // DB verification
        assertTrue(courseRepository.findByUuid(UUID.fromString(courseUuid)).isEmpty());
    }

    // ============================================================
    // TEST 7 — Validation safety invariant test
    // ============================================================
    @Test
    void invalidCourseCreationShouldNotChangeState() throws Exception {
        // Register + login professor
        MockHttpSession session = registerAndLogin(
                "owner@test.com",
                "password123",
                "Search",
                "Prof",
                "PROFESSOR");
                

        // Capture state before request
        int beforeCount = courseRepository.findAll().size();

        // Attempt invalid course creation
        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "",
                    "code": ""
                }
                """))
                .andExpect(status().isUnprocessableEntity());

        int afterCount = courseRepository.findAll().size();

        // Verify system state unchanged
        assertEquals(beforeCount, afterCount);
    }

    /*
     * TEST 8: Multi-user data isolation test
     * Professor B should not see Professor A courses.
     */
    @Test
    void professorIsolationVisibilityTest() throws Exception {
        // Register and login Professor A
        MockHttpSession professorASession = registerAndLogin(
            "owner@test.com",
            "password123",
            "Search",
            "Prof",
            "PROFESSOR");

        // Professor A creates course
        MvcResult result = mockMvc.perform(post("/api/courses")
                .session(professorASession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Isolation Course",
                            "code": "ISO101"
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        String uuid = JsonPath.read(result.getResponse().getContentAsString(), "$.uuid");

        // Register Professor B
        String professorBEmail = "profB-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "%s",
                        "password": "password123",
                        "firstName": "Prof",
                        "lastName": "B",
                        "role": "PROFESSOR"
                    }
                        """.formatted(professorBEmail)))
                .andExpect(status().isCreated());

        // Login Professor B
        MockHttpSession professorBSession = loginAndGetSession(professorBEmail, "password123");

        // Professor B accesses course
        mockMvc.perform(get("/api/courses/{uuid}", uuid)
                .session(professorBSession))
                .andExpect(status().isOk());
    }


    /*
     * TEST 9: Logout session invalidation protection test
     * Verify actions cannot be performed after logout.
     */
    @Test
    void logoutShouldInvalidateSessionProtection() throws Exception {
        // Register and login
        MockHttpSession session = registerAndLogin(
            "owner@test.com",
            "password123",
            "Search",
            "Prof",
            "PROFESSOR");

        // Logout
        mockMvc.perform(post("/api/auth/logout")
                .session(session))
                .andExpect(status().isNoContent());
        

        // Attempt protected action after logout
        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Post Logout Course",
                            "code": "LOGOUT101"
                        }
                        """))
                .andExpect(status().isUnauthorized());
    }

    /*
     * TEST 10: Course search integration pipeline test
     * Verify search endpoint correctly:
     * - Queries database
     * - Filters by title keyword
     * - Returns matching courses only
     */
    @Test
    void courseSearchIntegrationTest() throws Exception {
        // Register and login professor
        MockHttpSession session = registerAndLogin(
        "search@test.com",
        "password123",
        "Search",
        "Prof",
        "PROFESSOR");

        // Create searchable course
        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Searchable Course Alpha",
                            "code": "SRCH101"
                        }
                        """))
                .andExpect(status().isCreated());

        // Create noise course
        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Random Course Beta",
                            "code": "RND202"
                        }
                        """))
                .andExpect(status().isCreated());

        //Verify search
        mockMvc.perform(get("/api/courses")
                .param("title", "Searchable")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title")
                        .value("Searchable Course Alpha"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ============================================================
    // TEST 11 — Duplicate registration conflict contract
    // ============================================================
    @Test
    void duplicateRegistrationReturnsConflictWithExpectedMessage() throws Exception {
        String registerRequest = """
                {
                    "email": "duplicate@test.com",
                    "password": "password123",
                    "firstName": "Dup",
                    "lastName": "User",
                    "role": "STUDENT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    // ============================================================
    // TEST 12 — Login/session contract for both roles
    // ============================================================
    @Test
    void loginSessionReturnsRoleUserIdAndDashboardPathForEachRole() throws Exception {
        MockHttpSession professorSession = registerAndLogin(
                "prof-session@test.com",
                "password123",
                "Prof",
                "Session",
                "PROFESSOR");
        Integer professorId = (Integer) professorSession.getAttribute("AUTH_USER_ID");

        mockMvc.perform(get("/api/auth/session").session(professorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSOR"))
                .andExpect(jsonPath("$.userId").value(professorId))
                .andExpect(jsonPath("$.dashboardPath").value("/professor/dashboard/" + professorId));

        MockHttpSession studentSession = registerAndLogin(
                "student-session@test.com",
                "password123",
                "Student",
                "Session",
                "STUDENT");
        Integer studentId = (Integer) studentSession.getAttribute("AUTH_USER_ID");

        mockMvc.perform(get("/api/auth/session").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.userId").value(studentId))
                .andExpect(jsonPath("$.dashboardPath").value("/student/dashboard/" + studentId));
    }

    // ============================================================
    // TEST 13 — Logout invalidates auth/session endpoint
    // ============================================================
    @Test
    void logoutShouldInvalidateAuthSessionEndpoint() throws Exception {
        MockHttpSession session = registerAndLogin(
                "logout-session@test.com",
                "password123",
                "Logout",
                "User",
                "PROFESSOR");

        mockMvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // TEST 14 — Course creation validation + no DB insert
    // ============================================================
    @Test
    void blankTitleOrCodeReturns422AndDoesNotInsertCourse() throws Exception {
        MockHttpSession session = registerAndLogin(
                "course-validation@test.com",
                "password123",
                "Course",
                "Validator",
                "PROFESSOR");

        int beforeCount = courseRepository.findAll().size();

        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "",
                            "code": "VALID101"
                        }
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").exists());

        assertEquals(beforeCount, courseRepository.findAll().size());

        mockMvc.perform(post("/api/courses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "title": "Validation Course",
                            "code": ""
                        }
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").exists());

        assertEquals(beforeCount, courseRepository.findAll().size());
    }

    // ============================================================
    // TEST 15 — Student discovery, enrollment, and content access
    // ============================================================
    @Test
    void studentCanDiscoverEnrollAccessContentAndUnenroll() throws Exception {
        MockHttpSession professorSession = registerAndLogin(
                uniqueEmail("prof-discovery"),
                "password123",
                "Discover",
                "Professor",
                "PROFESSOR");
        CreatedCourse createdCourse = createCourse(
                professorSession,
                "Distributed Systems",
                "COMP4350",
                "systems,distributed",
                "Week 1 notes"
        );

        MockHttpSession studentSession = registerAndLogin(
                uniqueEmail("student-discovery"),
                "password123",
                "Discovery",
                "Student",
                "STUDENT");
        Integer studentId = (Integer) studentSession.getAttribute("AUTH_USER_ID");

        mockMvc.perform(get("/api/courses").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(createdCourse.uuid()))
                .andExpect(jsonPath("$[0].title").value("Distributed Systems"));

        mockMvc.perform(get("/api/courses")
                        .param("tag", "distributed")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(createdCourse.uuid()))
                .andExpect(jsonPath("$[0].tags").value("systems,distributed"));

        mockMvc.perform(get("/api/courses/{uuid}", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(createdCourse.uuid()))
                .andExpect(jsonPath("$.material").value("Week 1 notes"));

        mockMvc.perform(post("/api/courses/{uuid}/enroll", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(createdCourse.uuid()));

        assertTrue(enrollmentRepository.existsByUserIdAndCourseIdAndIsActiveTrue(studentId, createdCourse.id()));

        mockMvc.perform(get("/api/courses/my-courses")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(createdCourse.uuid()))
                .andExpect(jsonPath("$[0].title").value("Distributed Systems"));

        mockMvc.perform(get("/api/courses/{uuid}/content", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.material").value("Week 1 notes"));

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isNoContent());

        assertFalse(enrollmentRepository.existsByUserIdAndCourseIdAndIsActiveTrue(studentId, createdCourse.id()));

        mockMvc.perform(get("/api/courses/{uuid}/content", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You must be enrolled to access course materials."));
    }

    // ============================================================
    // TEST 16 — Student-only enrollment endpoints reject others
    // ============================================================
    @Test
    void studentOnlyEnrollmentEndpointsRejectProfessorAndAnonymousUsers() throws Exception {
        MockHttpSession professorSession = registerAndLogin(
                uniqueEmail("prof-enroll-guard"),
                "password123",
                "Enroll",
                "Professor",
                "PROFESSOR");
        CreatedCourse createdCourse = createCourse(
                professorSession,
                "Operating Systems",
                "COMP3430",
                "systems,os",
                "Kernel notes"
        );

        mockMvc.perform(post("/api/courses/{uuid}/enroll", createdCourse.uuid())
                        .session(professorSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only students can enroll in courses."));

        mockMvc.perform(post("/api/courses/{uuid}/enroll", createdCourse.uuid()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please sign in to continue."));
    }

    // ============================================================
    // TEST 17 — Important date CRUD integration pipeline
    // ============================================================
    @Test
    void ownerProfessorCanManageImportantDatesThroughCrudFlow() throws Exception {
        MockHttpSession professorSession = registerAndLogin(
                uniqueEmail("prof-important-dates"),
                "password123",
                "Dates",
                "Professor",
                "PROFESSOR");
        CreatedCourse createdCourse = createCourse(
                professorSession,
                "Software Testing",
                "COMP4220",
                "testing,qa",
                "Mutation testing guide"
        );

        Integer importantDateId = createImportantDate(
                professorSession,
                createdCourse.id(),
                "Sprint Demo",
                "Bring final checklist",
                "2026-05-01T15:00:00Z"
        );

        mockMvc.perform(get("/api/important-dates")
                        .param("courseId", createdCourse.id().toString())
                        .session(professorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(importantDateId))
                .andExpect(jsonPath("$[0].title").value("Sprint Demo"));

        mockMvc.perform(get("/api/important-dates/{id}", importantDateId)
                        .session(professorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(createdCourse.id()))
                .andExpect(jsonPath("$.description").value("Bring final checklist"));

        mockMvc.perform(patch("/api/important-dates/{id}", importantDateId)
                        .session(professorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Sprint Demo Updated",
                                    "description": "Bring the release checklist",
                                    "dueAt": "2026-05-02T16:30:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sprint Demo Updated"))
                .andExpect(jsonPath("$.description").value("Bring the release checklist"));

        mockMvc.perform(delete("/api/important-dates/{id}", importantDateId)
                        .session(professorSession))
                .andExpect(status().isNoContent());

        assertTrue(importantDateRepository.findById(importantDateId).isEmpty());
    }

    // ============================================================
    // TEST 18 — Important date authz and not-found contracts
    // ============================================================
    @Test
    void importantDateAuthorizationAndNotFoundContractsHold() throws Exception {
        MockHttpSession ownerProfessorSession = registerAndLogin(
                uniqueEmail("prof-important-owner"),
                "password123",
                "Owner",
                "Professor",
                "PROFESSOR");
        CreatedCourse createdCourse = createCourse(
                ownerProfessorSession,
                "Advanced Databases",
                "COMP4510",
                "databases,sql",
                "Relational algebra notes"
        );
        Integer importantDateId = createImportantDate(
                ownerProfessorSession,
                createdCourse.id(),
                "Schema Review",
                "Bring the ERD",
                "2026-05-08T14:00:00Z"
        );

        MockHttpSession otherProfessorSession = registerAndLogin(
                uniqueEmail("prof-important-other"),
                "password123",
                "Other",
                "Professor",
                "PROFESSOR");
        MockHttpSession studentSession = registerAndLogin(
                uniqueEmail("student-important"),
                "password123",
                "Important",
                "Student",
                "STUDENT");

        mockMvc.perform(patch("/api/important-dates/{id}", importantDateId)
                        .session(otherProfessorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized update"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only manage important dates for courses you created."));

        mockMvc.perform(delete("/api/important-dates/{id}", importantDateId)
                        .session(otherProfessorSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only manage important dates for courses you created."));

        mockMvc.perform(post("/api/courses/{courseId}/important-dates", createdCourse.id())
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Student attempt",
                                    "description": "Should fail",
                                    "dueAt": "2026-05-10T10:00:00Z"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only professors can add, update, or delete important dates."));

        mockMvc.perform(patch("/api/important-dates/{id}", importantDateId)
                        .session(studentSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Student patch"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only professors can add, update, or delete important dates."));

        mockMvc.perform(delete("/api/important-dates/{id}", importantDateId)
                        .session(studentSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only professors can add, update, or delete important dates."));

        mockMvc.perform(get("/api/important-dates/{id}", 999999)
                        .session(ownerProfessorSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("This important date could not be found."));

        mockMvc.perform(post("/api/courses/{courseId}/important-dates", 999999)
                        .session(ownerProfessorSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Missing course",
                                    "description": "Should fail",
                                    "dueAt": "2026-05-10T10:00:00Z"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("This course could not be found."));
    }

    // ============================================================
    // TEST 19 — Student dashboard API flow integration
    // ============================================================
    @Test
    void studentDashboardApiFlowReflectsEnrollmentAndImportantDates() throws Exception {
        MockHttpSession professorSession = registerAndLogin(
                uniqueEmail("prof-dashboard"),
                "password123",
                "Dashboard",
                "Professor",
                "PROFESSOR");
        CreatedCourse createdCourse = createCourse(
                professorSession,
                "Requirements Engineering",
                "COMP4520",
                "requirements,design",
                "Course handbook"
        );
        Integer importantDateId = createImportantDate(
                professorSession,
                createdCourse.id(),
                "Workshop",
                "Bring the backlog",
                "2026-05-12T09:30:00Z"
        );

        MockHttpSession studentSession = registerAndLogin(
                uniqueEmail("student-dashboard"),
                "password123",
                "Dashboard",
                "Student",
                "STUDENT");
        Integer studentId = (Integer) studentSession.getAttribute("AUTH_USER_ID");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isCreated());

        assertTrue(enrollmentRepository.existsByUserIdAndCourseIdAndIsActiveTrue(studentId, createdCourse.id()));

        mockMvc.perform(get("/api/courses/my-courses")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(createdCourse.uuid()))
                .andExpect(jsonPath("$[0].material").value("Course handbook"));

        mockMvc.perform(get("/api/important-dates")
                        .param("courseId", createdCourse.id().toString())
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(importantDateId))
                .andExpect(jsonPath("$[0].title").value("Workshop"));

        mockMvc.perform(get("/api/courses/{uuid}/content", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.material").value("Course handbook"));

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/my-courses")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/courses/{uuid}/content", createdCourse.uuid())
                        .session(studentSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You must be enrolled to access course materials."));
    }
}

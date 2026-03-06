package com.example.backend.course;

import com.example.backend.controller.CourseController;
import com.example.backend.controller.GlobalExceptionHandler;
import com.example.backend.dto.CourseDto;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private static final UUID COURSE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String UPDATE_PAYLOAD = """
            {
              "description": "Updated content",
              "tags": "db,sql",
              "material": "Week 4 slides",
              "dueDate": "2026-04-01T10:00:00Z",
              "link": "https://example.com/new"
            }
            """;

    private static final String VALID_CREATE_PAYLOAD = """
            {
              "title": "Intro to Java",
              "code": "COMP101",
              "description": "Core Java concepts",
              "link": "https://example.com/java"
            }
            """;

    @Mock
    private CourseService courseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CourseController controller = new CourseController(courseService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCourseAsProfessorReturns201AndResponse() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        CourseDto.Response response = CourseDto.Response.builder()
                .id(1)
                .uuid(COURSE_UUID)
                .title("Intro to Java")
                .code("COMP101")
                .description("Core Java concepts")
                .link("https://example.com/java")
                .professorId(7)
                .build();

        when(courseService.create(any(CourseDto.CreateRequest.class), eq(7))).thenReturn(response);

        mockMvc.perform(post("/api/courses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(VALID_CREATE_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Intro to Java"))
                .andExpect(jsonPath("$.code").value("COMP101"))
                .andExpect(jsonPath("$.professorId").value(7));

        verify(courseService).create(any(CourseDto.CreateRequest.class), eq(7));
    }

    @Test
    void createCourseAsStudentReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 9);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        mockMvc.perform(post("/api/courses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(VALID_CREATE_PAYLOAD))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("Only professors can manage courses."));

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourseWithoutSessionReturns401() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(VALID_CREATE_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourseWithMissingRoleReturns401() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);

        mockMvc.perform(post("/api/courses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(VALID_CREATE_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourseWithMissingUserIdReturns401() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(post("/api/courses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(VALID_CREATE_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourseWithBlankTitleReturns422() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(post("/api/courses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "title": "   ",
                                  "code": "COMP101",
                                  "description": "Core Java concepts",
                                  "link": "https://example.com/java"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("must not be blank"));

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourseWithBlankCodeReturns422() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(post("/api/courses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "title": "Intro to Java",
                                  "code": "   ",
                                  "description": "Core Java concepts",
                                  "link": "https://example.com/java"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("must not be blank"));

        verifyNoInteractions(courseService);
    }

    @Test
    void updateAsOwnerProfessorReturns200AndResponseBody() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        CourseDto.Response response = CourseDto.Response.builder()
                .id(1)
                .uuid(COURSE_UUID)
                .title("Databases")
                .code("COMP4350")
                .description("Updated content")
                .tags("db,sql")
                .material("Week 4 slides")
                .dueDate(OffsetDateTime.parse("2026-04-01T10:00:00Z"))
                .link("https://example.com/new")
                .professorId(7)
                .build();

        when(courseService.update(eq(COURSE_UUID), any(CourseDto.UpdateRequest.class), eq(7))).thenReturn(response);

        mockMvc.perform(patch("/api/courses/{uuid}", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(UPDATE_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uuid").value(COURSE_UUID.toString()))
                .andExpect(jsonPath("$.description").value("Updated content"))
                .andExpect(jsonPath("$.tags").value("db,sql"))
                .andExpect(jsonPath("$.material").value("Week 4 slides"))
                .andExpect(jsonPath("$.professorId").value(7));

        verify(courseService).update(eq(COURSE_UUID), any(CourseDto.UpdateRequest.class), eq(7));
    }

    @Test
    void updateAsNonOwnerReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 9);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        when(courseService.update(eq(COURSE_UUID), any(CourseDto.UpdateRequest.class), eq(9)))
                .thenThrow(new CourseAccessDeniedException("You can only modify your own courses."));

        mockMvc.perform(patch("/api/courses/{uuid}", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(UPDATE_PAYLOAD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only modify your own courses."));
    }

    @Test
    void updateAsStudentReturns403WithoutCallingService() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 9);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        mockMvc.perform(patch("/api/courses/{uuid}", COURSE_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(UPDATE_PAYLOAD))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("Only professors can manage courses."));

        verifyNoInteractions(courseService);
    }

    @Test
    void deleteAsOwnerProfessorReturns204() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(delete("/api/courses/{uuid}", COURSE_UUID)
                        .session(session))
                .andExpect(status().isNoContent());

        verify(courseService).delete(COURSE_UUID, 7);
    }

    @Test
    void deleteWithoutSessionReturns401WithoutCallingService() throws Exception {
        mockMvc.perform(delete("/api/courses/{uuid}", COURSE_UUID))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(courseService);
    }
}

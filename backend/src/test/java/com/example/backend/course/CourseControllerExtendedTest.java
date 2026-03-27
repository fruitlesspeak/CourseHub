package com.example.backend.course;

import com.example.backend.controller.CourseController;
import com.example.backend.controller.GlobalExceptionHandler;
import com.example.backend.dto.CourseDto;
import com.example.backend.service.CourseService;
import com.example.backend.service.EnrollmentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerExtendedTest {

    private static final UUID COURSE_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private CourseService     courseService;
    @Mock private EnrollmentService enrollmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CourseController controller = new CourseController(courseService, enrollmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listWithNoParamsReturnsAllCourses() throws Exception {
        CourseDto.Response c1 = CourseDto.Response.builder().id(1).uuid(COURSE_UUID).title("Java").build();
        CourseDto.Response c2 = CourseDto.Response.builder().id(2).uuid(UUID.randomUUID()).title("Python").build();

        when(courseService.findAll()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Java"))
                .andExpect(jsonPath("$[1].title").value("Python"));

        verify(courseService).findAll();
        verify(courseService, never()).search(any());
        verify(courseService, never()).findByProfessor(any());
        verify(courseService, never()).findByTag(any());
    }

    @Test
    void listWithProfessorIdParamCallsFindByProfessor() throws Exception {
        CourseDto.Response course = CourseDto.Response.builder()
                .id(1).uuid(COURSE_UUID).title("Databases").professorId(7).build();

        when(courseService.findByProfessor(7)).thenReturn(List.of(course));

        mockMvc.perform(get("/api/courses").param("professorId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].professorId").value(7));

        verify(courseService).findByProfessor(7);
    }

    @Test
    void listWithTitleParamCallsSearch() throws Exception {
        CourseDto.Response course = CourseDto.Response.builder()
                .id(1).uuid(COURSE_UUID).title("Intro to Databases").build();

        when(courseService.search("Databases")).thenReturn(List.of(course));

        mockMvc.perform(get("/api/courses").param("title", "Databases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Intro to Databases"));

        verify(courseService).search("Databases");
    }

    @Test
    void listWithBlankTitleFallsBackToFindAll() throws Exception {
        when(courseService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/courses").param("title", "   "))
                .andExpect(status().isOk());

        verify(courseService).findAll();
        verify(courseService, never()).search(any());
    }

    @Test
    void listWithBlankTagFallsBackToFindAll() throws Exception {
        when(courseService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/courses").param("tag", "   "))
                .andExpect(status().isOk());

        verify(courseService).findAll();
        verify(courseService, never()).findByTag(any());
    }

    @Test
    void getByUuidReturns200WithCourseBodyNoSessionRequired() throws Exception {
        CourseDto.Response course = CourseDto.Response.builder()
                .id(1).uuid(COURSE_UUID).title("Databases").professorId(7).build();

        when(courseService.findByUuid(COURSE_UUID)).thenReturn(course);

        mockMvc.perform(get("/api/courses/{uuid}", COURSE_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(COURSE_UUID.toString()))
                .andExpect(jsonPath("$.title").value("Databases"));
    }

    @Test
    void getByUuidReturns404WhenCourseNotFound() throws Exception {
        when(courseService.findByUuid(COURSE_UUID))
                .thenThrow(new EntityNotFoundException("Course not found: " + COURSE_UUID));

        mockMvc.perform(get("/api/courses/{uuid}", COURSE_UUID))
                .andExpect(status().isNotFound());
    }

    @Test
    void enrollWithoutSessionReturns401() throws Exception {
        mockMvc.perform(post("/api/courses/{uuid}/enroll", COURSE_UUID))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void enrollAsProfessorReturns403WithoutCallingEnrollmentService() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(post("/api/courses/{uuid}/enroll", COURSE_UUID).session(session))
                .andExpect(status().isForbidden());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void enrollAlreadyActiveThrowsConflictMappedByGlobalHandler() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   5);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        CourseDto.Response course = CourseDto.Response.builder()
                .id(1).uuid(COURSE_UUID).title("Databases").build();

        when(courseService.findByUuid(COURSE_UUID)).thenReturn(course);
        doThrow(new IllegalStateException("User already enrolled in this course"))
                .when(enrollmentService).enrollOrReactivate(5, 1);

        mockMvc.perform(post("/api/courses/{uuid}/enroll", COURSE_UUID).session(session))
                .andExpect(status().isConflict());
    }

    @Test
    void unenrollAsStudentReturns204AndCallsDeactivate() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   5);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        CourseDto.Response course = CourseDto.Response.builder()
                .id(1).uuid(COURSE_UUID).title("Databases").build();

        when(courseService.findByUuid(COURSE_UUID)).thenReturn(course);

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", COURSE_UUID).session(session))
                .andExpect(status().isNoContent());

        verify(enrollmentService).deactivate(5, 1);
    }

    @Test
    void unenrollWithoutSessionReturns401() throws Exception {
        mockMvc.perform(delete("/api/courses/{uuid}/enroll", COURSE_UUID))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void unenrollAsProfessorReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", COURSE_UUID).session(session))
                .andExpect(status().isForbidden());

        verifyNoInteractions(enrollmentService);
    }

    @Test
    void unenrollNotEnrolledReturns404MappedByGlobalHandler() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   5);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        CourseDto.Response course = CourseDto.Response.builder()
                .id(1).uuid(COURSE_UUID).title("Databases").build();

        when(courseService.findByUuid(COURSE_UUID)).thenReturn(course);
        doThrow(new EntityNotFoundException("Enrollment not found"))
                .when(enrollmentService).deactivate(5, 1);

        mockMvc.perform(delete("/api/courses/{uuid}/enroll", COURSE_UUID).session(session))
                .andExpect(status().isNotFound());
    }


    @Test
    void myCoursesAsProfessorReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(get("/api/courses/my-courses").session(session))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    void myCoursesReturnsEmptyListWhenStudentHasNoEnrollments() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   5);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        when(courseService.findMyCourses(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/courses/my-courses").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }


    @Test
    void getContentAsProfessorReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(get("/api/courses/{uuid}/content", COURSE_UUID).session(session))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
        verifyNoInteractions(enrollmentService);
    }

    @Test
    void getContentAsEnrolledStudentReturnsMaterialWhenNull() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   5);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        CourseDto.Response course = CourseDto.Response.builder()
                .id(1).uuid(COURSE_UUID).title("Java").material(null).build();

        when(courseService.findByUuid(COURSE_UUID)).thenReturn(course);
        when(enrollmentService.isEnrolled(5, 1)).thenReturn(true);

        // null material must return empty string
        mockMvc.perform(get("/api/courses/{uuid}/content", COURSE_UUID).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.material").value(""));
    }

    @Test
    void updateWithoutSessionReturns401() throws Exception {
        mockMvc.perform(patch("/api/courses/{uuid}", COURSE_UUID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(courseService);
    }

    @Test
    void updateCourseNotFoundReturns404() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        when(courseService.update(eq(COURSE_UUID), any(), eq(7)))
                .thenThrow(new EntityNotFoundException("Course not found: " + COURSE_UUID));

        mockMvc.perform(patch("/api/courses/{uuid}", COURSE_UUID)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCourseNotFoundReturns404() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        doThrow(new EntityNotFoundException("Course not found: " + COURSE_UUID))
                .when(courseService).delete(COURSE_UUID, 7);

        mockMvc.perform(delete("/api/courses/{uuid}", COURSE_UUID).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAsStudentReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID",   5);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        mockMvc.perform(delete("/api/courses/{uuid}", COURSE_UUID).session(session))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("Only professors can manage courses."));

        verifyNoInteractions(courseService);
    }
}

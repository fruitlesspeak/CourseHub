package com.example.backend.importantdate;

import com.example.backend.controller.GlobalExceptionHandler;
import com.example.backend.controller.ImportantDateController;
import com.example.backend.dto.ImportantDateDto;
import com.example.backend.service.ImportantDateService;
import com.example.backend.service.SessionAuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImportantDateControllerTest {

    private static final String CREATE_PAYLOAD = """
            {
              "title": "Midterm review",
              "description": "Bring your questions",
              "dueAt": "2026-04-01T10:00:00Z"
            }
            """;

    @Mock
    private ImportantDateService importantDateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ImportantDateController controller = new ImportantDateController(
                importantDateService,
                new SessionAuthService()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createAsProfessorReturns201() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        ImportantDateDto.Response response = ImportantDateDto.Response.builder()
                .id(1)
                .courseId(5)
                .createdByUserId(7)
                .title("Midterm review")
                .description("Bring your questions")
                .dueAt(OffsetDateTime.parse("2026-04-01T10:00:00Z"))
                .build();

        when(importantDateService.create(eq(5), any(ImportantDateDto.CreateRequest.class), eq(7)))
                .thenReturn(response);

        mockMvc.perform(post("/api/courses/{courseId}/important-dates", 5)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(CREATE_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(5))
                .andExpect(jsonPath("$.createdByUserId").value(7))
                .andExpect(jsonPath("$.title").value("Midterm review"));

        verify(importantDateService).create(eq(5), any(ImportantDateDto.CreateRequest.class), eq(7));
    }

    @Test
    void createWithoutSessionReturns401() throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/important-dates", 5)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(CREATE_PAYLOAD))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(importantDateService);
    }

    @Test
    void createAsStudentReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 9);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        mockMvc.perform(post("/api/courses/{courseId}/important-dates", 5)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(CREATE_PAYLOAD))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("Only professors can manage important dates."));

        verifyNoInteractions(importantDateService);
    }

    @Test
    void updateWithInvalidRoleInSessionReturns401() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 9);
        session.setAttribute("AUTH_USER_ROLE", "INVALID_ROLE");

        mockMvc.perform(patch("/api/important-dates/{id}", 3)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "title": "Updated title"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(importantDateService);
    }

    @Test
    void deleteAsProfessorReturns204() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "PROFESSOR");

        mockMvc.perform(delete("/api/important-dates/{id}", 3)
                        .session(session))
                .andExpect(status().isNoContent());

        verify(importantDateService).delete(3, 7);
    }
}

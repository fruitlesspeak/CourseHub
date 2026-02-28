package com.example.backend.auth;

import com.example.backend.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(AuthExceptionHandler.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @Test
        void loginWithValidPayloadReturns200AndResponseBody() throws Exception {
                when(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                                .thenReturn(new LoginResponse(1L, UserRole.STUDENT, "/student/dashboard/1"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content("""
                                                {
                                                  "email": "demo@student.coursehub",
                                                  "password": "CourseHub123!"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(jsonPath("$.userId").value(1))
                                .andExpect(jsonPath("$.role").value("STUDENT"))
                                .andExpect(jsonPath("$.dashboardPath").value("/student/dashboard/1"));

                verify(authService).login(any(LoginRequest.class), any(HttpServletRequest.class));
        }

        @Test
        void loginWithInvalidCredentialsReturns401() throws Exception {
                when(authService.login(any(LoginRequest.class), any(HttpServletRequest.class)))
                                .thenThrow(new InvalidCredentialsException());

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content("""
                                                {
                                                  "email": "demo@student.coursehub",
                                                  "password": "WrongPassword"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        }

        @Test
        void loginWithInvalidPayloadReturns400() throws Exception {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content("""
                                                {
                                                  "email": "not-an-email",
                                                  "password": "short"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Invalid login payload."));

                verifyNoInteractions(authService);
        }

        @Test
        void sessionWhenAuthenticatedReturns200() throws Exception {
                when(authService.getCurrentSession(any(HttpServletRequest.class)))
                                .thenReturn(Optional.of(new AuthSessionResponse(
                                                1L,
                                                "Student User",
                                                "student@coursehub.test",
                                                UserRole.STUDENT,
                                                "/student/dashboard/1"
                                )));

                mockMvc.perform(get("/api/auth/session"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(jsonPath("$.userId").value(1))
                                .andExpect(jsonPath("$.name").value("Student User"))
                                .andExpect(jsonPath("$.role").value("STUDENT"));
        }

        @Test
        void sessionWhenUnauthenticatedReturns401() throws Exception {
                when(authService.getCurrentSession(any(HttpServletRequest.class))).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/auth/session"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void logoutReturns204() throws Exception {
                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isNoContent());

                verify(authService).logout(any(HttpServletRequest.class));
        }
}

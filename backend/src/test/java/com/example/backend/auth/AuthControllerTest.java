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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
                                .thenReturn(new LoginResponse(1L, UserRole.STUDENT, "/dashboard/1"));

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
                                .andExpect(jsonPath("$.dashboardPath").value("/dashboard/1"));

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
}

package com.example.backend.register;

import com.example.backend.controller.RegisterController;
import com.example.backend.entity.User;
import com.example.backend.entity.UserRole;
import com.example.backend.exception.ApiExceptionHandler;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.RegisterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RegisterService registerService = new RegisterService(userRepository, passwordEncoder);
        RegisterController registerController = new RegisterController(registerService);
        mockMvc = MockMvcBuilders.standaloneSetup(registerController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void registerWithValidPayloadReturns201AndResponseBody() throws Exception {
        when(userRepository.existsByEmail("demo@student.coursehub")).thenReturn(false);
        when(passwordEncoder.encode("CourseHub123!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0, User.class);
            saved.setId(1);
            return saved;
        });

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "firstName": "Demo",
                                  "lastName": "Student",
                                  "email": "demo@student.coursehub",
                                  "password": "CourseHub123!",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Demo"))
                .andExpect(jsonPath("$.lastName").value("Student"))
                .andExpect(jsonPath("$.email").value("demo@student.coursehub"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        verify(userRepository).existsByEmail("demo@student.coursehub");
        verify(passwordEncoder).encode("CourseHub123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        when(userRepository.existsByEmail("demo@student.coursehub")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "firstName": "Demo",
                                  "lastName": "Student",
                                  "email": "demo@student.coursehub",
                                  "password": "CourseHub123!",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"));

        verify(userRepository).existsByEmail("demo@student.coursehub");
        verifyNoInteractions(passwordEncoder);
    }
}

package com.example.backend.user;

import com.example.backend.controller.GlobalExceptionHandler;
import com.example.backend.controller.UserController;
import com.example.backend.dto.UserDto;
import com.example.backend.service.SessionAuthService;
import com.example.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UUID USER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService, new SessionAuthService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getOwnProfileReturns200() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        UserDto.Response response = UserDto.Response.builder()
                .id(7)
                .uuid(USER_UUID)
                .email("student@test.com")
                .firstName("Test")
                .lastName("Student")
                .isProfessor(false)
                .build();

        when(userService.findUserIdByUuid(USER_UUID)).thenReturn(7);
        when(userService.findByUuid(USER_UUID)).thenReturn(response);

        mockMvc.perform(get("/api/users/{uuid}", USER_UUID).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("student@test.com"));

        verify(userService).findUserIdByUuid(USER_UUID);
        verify(userService).findByUuid(USER_UUID);
    }

    @Test
    void getWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/users/{uuid}", USER_UUID))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Authentication required."));

        verifyNoInteractions(userService);
    }

    @Test
    void getOtherUsersProfileReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        when(userService.findUserIdByUuid(USER_UUID)).thenReturn(9);

        mockMvc.perform(get("/api/users/{uuid}", USER_UUID).session(session))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("You can only access your own user profile."));

        verify(userService).findUserIdByUuid(USER_UUID);
    }

    @Test
    void updateOtherUsersProfileReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        when(userService.findUserIdByUuid(USER_UUID)).thenReturn(9);

        mockMvc.perform(patch("/api/users/{uuid}", USER_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "firstName": "Updated"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("You can only access your own user profile."));
    }

    @Test
    void listForAuthenticatedUserReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        mockMvc.perform(get("/api/users").session(session))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("User listing is not available."));

        verifyNoInteractions(userService);
    }

    @Test
    void createForAuthenticatedUserReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        mockMvc.perform(post("/api/users")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "password123",
                                  "firstName": "User",
                                  "lastName": "Test"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("User creation is only available through registration."));

        verifyNoInteractions(userService);
    }

    @Test
    void deleteForAuthenticatedUserReturns403() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        mockMvc.perform(delete("/api/users/{uuid}", USER_UUID).session(session))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("User deletion is not available."));

        verifyNoInteractions(userService);
    }

    @Test
    void updateOwnProfileReturns200() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("AUTH_USER_ID", 7);
        session.setAttribute("AUTH_USER_ROLE", "STUDENT");

        UserDto.Response response = UserDto.Response.builder()
                .id(7)
                .uuid(USER_UUID)
                .email("student@test.com")
                .firstName("Updated")
                .lastName("Student")
                .isProfessor(false)
                .build();

        when(userService.findUserIdByUuid(USER_UUID)).thenReturn(7);
        when(userService.update(eq(USER_UUID), any(UserDto.UpdateRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/users/{uuid}", USER_UUID)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                  "firstName": "Updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));

        verify(userService).findUserIdByUuid(USER_UUID);
        verify(userService).update(eq(USER_UUID), any(UserDto.UpdateRequest.class));
    }
}

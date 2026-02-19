package com.example.backend.auth;

import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import com.example.backend.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void loginWithValidCredentialsCreatesSessionAndReturnsDashboard() {
        LoginRequest request = new LoginRequest("  DEMO@student.coursehub  ", "CourseHub123!");

        User user = buildUser(1L, "demo@student.coursehub", "hashed-password", UserRole.STUDENT);
        when(userRepository.findByEmailIgnoreCase("demo@student.coursehub")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CourseHub123!", "hashed-password")).thenReturn(true);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        LoginResponse response = authService.login(request, httpRequest);

        assertEquals(1L, response.userId());
        assertEquals(UserRole.STUDENT, response.role());
        assertEquals("/dashboard/1", response.dashboardPath());

        var session = httpRequest.getSession(false);
        assertNotNull(session);
        assertEquals(1L, session.getAttribute("AUTH_USER_ID"));
        assertEquals("STUDENT", session.getAttribute("AUTH_USER_ROLE"));

        verify(userRepository).findByEmailIgnoreCase("demo@student.coursehub");
        verify(passwordEncoder).matches("CourseHub123!", "hashed-password");
    }

    @Test
    void loginWithUnknownEmailThrowsInvalidCredentials() {
        LoginRequest request = new LoginRequest("unknown@coursehub.test", "CourseHub123!");
        when(userRepository.findByEmailIgnoreCase("unknown@coursehub.test")).thenReturn(Optional.empty());

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, httpRequest));
        assertNull(httpRequest.getSession(false));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginWithWrongPasswordThrowsInvalidCredentials() {
        LoginRequest request = new LoginRequest("demo@student.coursehub", "WrongPassword123");

        User user = buildUser(1L, "demo@student.coursehub", "hashed-password", UserRole.STUDENT);
        when(userRepository.findByEmailIgnoreCase("demo@student.coursehub")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword123", "hashed-password")).thenReturn(false);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, httpRequest));
        assertNull(httpRequest.getSession(false));
    }

    private static User buildUser(Long id, String email, String passwordHash, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName("Demo User");
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        return user;
    }
}

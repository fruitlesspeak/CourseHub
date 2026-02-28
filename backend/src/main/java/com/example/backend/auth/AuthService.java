package com.example.backend.auth;

import com.example.backend.user.User;
import com.example.backend.user.UserRepository;
import com.example.backend.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final String SESSION_USER_ID = "AUTH_USER_ID";
    private static final String SESSION_USER_ROLE = "AUTH_USER_ROLE";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USER_ROLE, user.getRole().name());

        String dashboardPath = buildDashboardPath(user.getRole(), user.getId());
        return new LoginResponse(user.getId(), user.getRole(), dashboardPath);
    }

    public Optional<AuthSessionResponse> getCurrentSession(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object sessionUserId = session.getAttribute(SESSION_USER_ID);
        if (!(sessionUserId instanceof Long userId)) {
            session.invalidate();
            return Optional.empty();
        }

        return userRepository.findById(userId).map(user -> new AuthSessionResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                buildDashboardPath(user.getRole(), user.getId())
        ));
    }

    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private static String buildDashboardPath(UserRole role, Long userId) {
        String prefix = role == UserRole.PROFESSOR ? "/professor/dashboard/" : "/student/dashboard/";
        return prefix + userId;
    }
}


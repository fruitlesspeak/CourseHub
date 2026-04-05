package com.example.backend.service;

import com.example.backend.entity.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessionAuthService {

    private static final String SESSION_USER_ID = "AUTH_USER_ID";
    private static final String SESSION_USER_ROLE = "AUTH_USER_ROLE";
    private static final String AUTH_REQUIRED_MESSAGE = "Please sign in to continue.";

    public SessionUser requireAuthenticatedUser(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AUTH_REQUIRED_MESSAGE);
        }

        Object sessionUserId = session.getAttribute(SESSION_USER_ID);
        if (!(sessionUserId instanceof Integer userId)) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AUTH_REQUIRED_MESSAGE);
        }

        Object sessionUserRole = session.getAttribute(SESSION_USER_ROLE);
        if (!(sessionUserRole instanceof String roleName)) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AUTH_REQUIRED_MESSAGE);
        }

        try {
            return new SessionUser(userId, UserRole.valueOf(roleName));
        } catch (IllegalArgumentException ex) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AUTH_REQUIRED_MESSAGE);
        }
    }

    public SessionUser requireProfessor(HttpServletRequest httpRequest, String forbiddenMessage) {
        SessionUser sessionUser = requireAuthenticatedUser(httpRequest);
        if (sessionUser.role() != UserRole.PROFESSOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }
        return sessionUser;
    }

    public record SessionUser(Integer userId, UserRole role) {
    }
}

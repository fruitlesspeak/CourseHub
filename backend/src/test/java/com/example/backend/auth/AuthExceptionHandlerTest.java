package com.example.backend.auth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void handleInvalidCredentialsReturnsUnauthorizedMessage() {
        Map<String, String> response = handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertEquals("Invalid email or password.", response.get("message"));
    }

    @Test
    void handleValidationErrorReturnsBadRequestMessage() {
        Map<String, String> response = handler.handleValidationError(null);

        assertEquals("Invalid login payload.", response.get("message"));
    }
}

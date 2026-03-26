package com.example.backend.auth;

import com.example.backend.exception.AuthExceptionHandler;
import com.example.backend.exception.InvalidCredentialsException;
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
}

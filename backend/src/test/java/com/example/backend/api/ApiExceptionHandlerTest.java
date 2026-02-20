package com.example.backend.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleEmailConflictReturnsConflictMessage() {
        ResponseEntity<?> response = handler.handleEmailConflict();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(Map.of("message", "Email already in use"), response.getBody());
    }
}

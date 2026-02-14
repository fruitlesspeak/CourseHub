package com.example.backend.api;

import com.example.backend.register.EmailAlreadyInUseException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<?> handleEmailConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Email already in use"));
    }
}

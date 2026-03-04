package com.example.backend.exception;

public class CourseAccessDeniedException extends RuntimeException {
    public CourseAccessDeniedException(String message) {
        super(message);
    }
}

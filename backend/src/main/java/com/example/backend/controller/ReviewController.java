package com.example.backend.controller;

import com.example.backend.dto.ReviewDto;
import com.example.backend.entity.UserRole;
import com.example.backend.service.ReviewService;
import com.example.backend.service.SessionAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class ReviewController {

    private final ReviewService      reviewService;
    private final SessionAuthService sessionAuthService;

    public ReviewController(ReviewService reviewService, SessionAuthService sessionAuthService) {
        this.reviewService      = reviewService;
        this.sessionAuthService = sessionAuthService;
    }

    /** POST /api/courses/{uuid}/reviews — enrolled student submits a review */
    @PostMapping("/{uuid}/reviews")
    public ResponseEntity<ReviewDto.Response> submit(
            @PathVariable UUID uuid,
            @Valid @RequestBody ReviewDto.CreateRequest req,
            HttpServletRequest httpRequest) {

        Integer studentId = resolveStudentId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.submit(req, studentId, uuid));
    }

    /** GET /api/courses/{uuid}/reviews — list reviews for a course */
    @GetMapping("/{uuid}/reviews")
    public ResponseEntity<List<ReviewDto.Response>> listByCourse(
            @PathVariable UUID uuid,
            HttpServletRequest httpRequest) {

        sessionAuthService.requireAuthenticatedUser(httpRequest);
        return ResponseEntity.ok(reviewService.findByCourse(uuid));
    }

    /** GET /api/courses/my-reviews — professor sees reviews across their courses */
    @GetMapping("/my-reviews")
    public ResponseEntity<List<ReviewDto.Response>> listForProfessor(
            HttpServletRequest httpRequest) {

        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can access this endpoint.")
                .userId();
        return ResponseEntity.ok(reviewService.findByProfessor(professorId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Integer resolveStudentId(HttpServletRequest httpRequest) {
        SessionAuthService.SessionUser sessionUser =
                sessionAuthService.requireAuthenticatedUser(httpRequest);
        if (sessionUser.role() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can submit reviews.");
        }
        return sessionUser.userId();
    }
}

package com.example.backend.controller;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.UserRole;
import com.example.backend.service.CourseService;
import com.example.backend.service.EnrollmentService;
import com.example.backend.service.ReviewService;
import com.example.backend.service.SessionAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/courses")
public class ReviewController {

    private final ReviewService reviewService;
    private final CourseService courseService;
    private final SessionAuthService sessionAuthService;
    private final EnrollmentService enrollmentService;
 

    public ReviewController(ReviewService reviewService, CourseService courseService, SessionAuthService sessionAuthService,
            EnrollmentService enrollmentService) {
        this.reviewService = reviewService;
        this.courseService = courseService;
        this.sessionAuthService = sessionAuthService;
        this.enrollmentService = enrollmentService;
    }

    /** POST /api/courses/{uuid}/reviews */
    @PostMapping
    public ResponseEntity<ReviewDto.Response> create(
            @Valid @RequestBody CourseDto.CreateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can create, update, or delete courses.")
                .userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(req, professorId));
    }

    /** GET /api/courses/{uuid}/reviews */
    @GetMapping
    public ResponseEntity<List<CourseDto.Response>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer professorId,
            @RequestParam(required = false) String tag) {

        List<CourseDto.Response> result;
        if (tag != null && !tag.isBlank())
            result = courseService.findByTag(tag);
        else if (title != null && !title.isBlank())
            result = courseService.search(title);
        else if (professorId != null)
            result = courseService.findByProfessor(professorId);
        else
            result = courseService.findAll();

        return ResponseEntity.ok(result);
    }
}

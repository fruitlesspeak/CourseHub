package com.example.backend.controller;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.UserRole;
import com.example.backend.service.CourseService;
import com.example.backend.service.SessionAuthService;
import com.example.backend.service.EnrollmentService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final SessionAuthService sessionAuthService;

    public CourseController(CourseService courseService, SessionAuthService sessionAuthService) {
        this.courseService = courseService;
        this.sessionAuthService = sessionAuthService;
    }

    /** POST /api/courses */
    @PostMapping
    public ResponseEntity<CourseDto.Response> create(
            @Valid @RequestBody CourseDto.CreateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can manage courses.")
                .userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(req, professorId));
    }

    /** GET /api/courses?title=&professorId= */
    @GetMapping
    public ResponseEntity<List<CourseDto.Response>> list(
            @RequestParam(required = false) String  title,
            @RequestParam(required = false) Integer professorId,
            @RequestParam(required = false) String tag) {

        List<CourseDto.Response> result;
        if (tag != null && !tag.isBlank())           result = courseService.findByTag(tag);
        else if (title != null && !title.isBlank())  result = courseService.search(title);
        else if (professorId != null)                result = courseService.findByProfessor(professorId);
        else                                         result = courseService.findAll();

        return ResponseEntity.ok(result);
    }

    /** GET /api/courses/{uuid} */
    @GetMapping("/{uuid}")
    public ResponseEntity<CourseDto.Response> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(courseService.findByUuid(uuid));
    }

    /** PATCH /api/courses/{uuid} */
    @PatchMapping("/{uuid}")
    public ResponseEntity<CourseDto.Response> update(
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseDto.UpdateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can manage courses.")
                .userId();
        return ResponseEntity.ok(courseService.update(uuid, req, professorId));
    }

    /** DELETE /api/courses/{uuid} */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid, HttpServletRequest httpRequest) {
        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can manage courses.")
                .userId();
        courseService.delete(uuid, professorId);
        return ResponseEntity.noContent().build();
    }

    /** DELETE /api/courses/{uuid}/enroll */
    @DeleteMapping("/{uuid}/enroll")
    public ResponseEntity<Void> unenroll(
            @PathVariable UUID uuid,
            HttpServletRequest httpRequest) {

        Integer studentId = resolveStudentIdFromSession(httpRequest);
        CourseDto.Response course = courseService.findByUuid(uuid);
        enrollmentService.deactivate(studentId, course.getId());
        return ResponseEntity.noContent().build();
    }

    private static Integer resolveProfessorIdFromSession(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        Object sessionUserId = session.getAttribute(SESSION_USER_ID);
        if (!(sessionUserId instanceof Integer userId)) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        Object sessionUserRole = session.getAttribute(SESSION_USER_ROLE);
        if (!(sessionUserRole instanceof String role)) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        if (!UserRole.PROFESSOR.name().equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professors can manage courses.");
        }

        return userId;
    }

    private static Integer resolveStudentIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        Object sessionUserId = session.getAttribute(SESSION_USER_ID);
        if (!(sessionUserId instanceof Integer userId)) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        Object sessionUserRole = session.getAttribute(SESSION_USER_ROLE);
        if (!(sessionUserRole instanceof String role)) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        if (!UserRole.STUDENT.name().equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can enroll.");
        }

        return userId;
    }
}

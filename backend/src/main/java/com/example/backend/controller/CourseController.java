package com.example.backend.controller;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.UserRole;
import com.example.backend.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final String SESSION_USER_ID = "AUTH_USER_ID";
    private static final String SESSION_USER_ROLE = "AUTH_USER_ROLE";

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** POST /api/courses */
    @PostMapping
    public ResponseEntity<CourseDto.Response> create(
            @Valid @RequestBody CourseDto.CreateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = resolveProfessorIdFromSession(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(req, professorId));
    }

    /** GET /api/courses?title=&professorId= */
    @GetMapping
    public ResponseEntity<List<CourseDto.Response>> list(
            @RequestParam(required = false) String  title,
            @RequestParam(required = false) Integer professorId) {

        List<CourseDto.Response> result;
        if (title != null && !title.isBlank()) result = courseService.search(title);
        else if (professorId != null)           result = courseService.findByProfessor(professorId);
        else                                    result = courseService.findAll();

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
            @Valid @RequestBody CourseDto.UpdateRequest req) {
        return ResponseEntity.ok(courseService.update(uuid, req));
    }

    /** DELETE /api/courses/{uuid} */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        courseService.delete(uuid);
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professors can create courses.");
        }

        return userId;
    }
}

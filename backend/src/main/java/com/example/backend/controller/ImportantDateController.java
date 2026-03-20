package com.example.backend.controller;

import com.example.backend.dto.ImportantDateDto;
import com.example.backend.entity.UserRole;
import com.example.backend.service.ImportantDateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class ImportantDateController {

    private static final String SESSION_USER_ID = "AUTH_USER_ID";
    private static final String SESSION_USER_ROLE = "AUTH_USER_ROLE";

    private final ImportantDateService importantDateService;

    public ImportantDateController(ImportantDateService importantDateService) {
        this.importantDateService = importantDateService;
    }

    // -- Create ----------------------------------------------------------------

    /** POST /api/courses/{courseId}/important-dates */
    @PostMapping("/api/courses/{courseId}/important-dates")
    public ResponseEntity<ImportantDateDto.Response> create(
            @PathVariable Integer courseId,
            @Valid @RequestBody ImportantDateDto.CreateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = resolveProfessorIdFromSession(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(importantDateService.create(courseId, req, professorId));
    }

    // -- Read ------------------------------------------------------------------

    /** GET /api/important-dates?courseId= */
    @GetMapping("/api/important-dates")
    public ResponseEntity<List<ImportantDateDto.Response>> list(
            @RequestParam Integer courseId) {
        return ResponseEntity.ok(importantDateService.findByCourse(courseId));
    }

    /** GET /api/important-dates/{id} */
    @GetMapping("/api/important-dates/{id}")
    public ResponseEntity<ImportantDateDto.Response> get(@PathVariable Integer id) {
        return ResponseEntity.ok(importantDateService.findById(id));
    }

    // -- Update ----------------------------------------------------------------

    /** PATCH /api/important-dates/{id} */
    @PatchMapping("/api/important-dates/{id}")
    public ResponseEntity<ImportantDateDto.Response> update(
            @PathVariable Integer id,
            @Valid @RequestBody ImportantDateDto.UpdateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = resolveProfessorIdFromSession(httpRequest);
        return ResponseEntity.ok(importantDateService.update(id, req, professorId));
    }

    // -- Delete ----------------------------------------------------------------

    /** DELETE /api/important-dates/{id} */
    @DeleteMapping("/api/important-dates/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer professorId = resolveProfessorIdFromSession(httpRequest);
        importantDateService.delete(id, professorId);
        return ResponseEntity.noContent().build();
    }

    // -- Helpers ---------------------------------------------------------------

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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professors can manage important dates.");
        }

        return userId;
    }
}

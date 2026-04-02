package com.example.backend.controller;

import com.example.backend.dto.ImportantDateDto;
import com.example.backend.service.ImportantDateService;
import com.example.backend.service.SessionAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ImportantDateController {

    private final ImportantDateService importantDateService;
    private final SessionAuthService sessionAuthService;

    public ImportantDateController(ImportantDateService importantDateService, SessionAuthService sessionAuthService) {
        this.importantDateService = importantDateService;
        this.sessionAuthService = sessionAuthService;
    }

    // -- Create ----------------------------------------------------------------

    /** POST /api/courses/{courseId}/important-dates */
    @PostMapping("/api/courses/{courseId}/important-dates")
    public ResponseEntity<ImportantDateDto.Response> create(
            @PathVariable Integer courseId,
            @Valid @RequestBody ImportantDateDto.CreateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can add, update, or delete important dates.")
                .userId();
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
        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can add, update, or delete important dates.")
                .userId();
        return ResponseEntity.ok(importantDateService.update(id, req, professorId));
    }

    // -- Delete ----------------------------------------------------------------

    /** DELETE /api/important-dates/{id} */
    @DeleteMapping("/api/important-dates/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, HttpServletRequest httpRequest) {
        Integer professorId = sessionAuthService
                .requireProfessor(httpRequest, "Only professors can add, update, or delete important dates.")
                .userId();
        importantDateService.delete(id, professorId);
        return ResponseEntity.noContent().build();
    }
}

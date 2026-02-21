package com.example.backend.controller;

import com.example.backend.dto.CourseDto;
import com.example.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** POST /api/courses */
    @PostMapping
    public ResponseEntity<CourseDto.Response> create(@Valid @RequestBody CourseDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(req));
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
}
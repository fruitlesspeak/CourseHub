package com.example.backend.controller;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.UserRole;
import com.example.backend.service.CourseService;
import com.example.backend.service.EnrollmentService;
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
public class CourseController {

    private final CourseService courseService;
    private final SessionAuthService sessionAuthService;
    private final EnrollmentService enrollmentService;

    public CourseController(CourseService courseService, SessionAuthService sessionAuthService, EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.sessionAuthService = sessionAuthService;
        this.enrollmentService = enrollmentService;
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

    /** POST /api/courses/{uuid}/enroll */
    @PostMapping("/{uuid}/enroll")
    public ResponseEntity<CourseDto.Response> enroll(
            @PathVariable UUID uuid,
            HttpServletRequest httpRequest) {
        Integer studentId = resolveStudentIdFromSession(httpRequest);
        CourseDto.Response course = courseService.findByUuid(uuid);
        enrollmentService.enrollOrReactivate(studentId, course.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
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

    /** GET /api/courses/my-courses */
    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseDto.Response>> getMyCourses(HttpServletRequest httpRequest) {
        Integer studentId = resolveStudentIdFromSession(httpRequest);
        return ResponseEntity.ok(courseService.findMyCourses(studentId));
    }

    /** GET /api/courses/{uuid}/content */
    @GetMapping("/{uuid}/content")
    public ResponseEntity<Map<String, String>> getCourseContent(
            @PathVariable UUID uuid,
            HttpServletRequest httpRequest) {
        Integer studentId = resolveStudentIdFromSession(httpRequest);
        CourseDto.Response course = courseService.findByUuid(uuid);

        if (!enrollmentService.isEnrolled(studentId, course.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You must be enrolled to access course materials."
            );
        }

        return ResponseEntity.ok(Map.of(
                "material",
                course.getMaterial() != null ? course.getMaterial() : ""
        ));
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

    private Integer resolveStudentIdFromSession(HttpServletRequest httpRequest) {
        SessionAuthService.SessionUser sessionUser = sessionAuthService.requireAuthenticatedUser(httpRequest);
        if (sessionUser.role() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can enroll.");
        }
        return sessionUser.userId();
    }
}

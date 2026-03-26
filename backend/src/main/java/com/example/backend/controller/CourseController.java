package com.example.backend.controller;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.UserRole;
import com.example.backend.service.CourseService;
import com.example.backend.service.EnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final String SESSION_USER_ID = "AUTH_USER_ID";
    private static final String SESSION_USER_ROLE = "AUTH_USER_ROLE";

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public CourseController(CourseService courseService, EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    } 

    /** POST /api/courses */
    @PostMapping
    public ResponseEntity<CourseDto.Response> create(
            @Valid @RequestBody CourseDto.CreateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = resolveProfessorIdFromSession(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(req, professorId));
    }

    /** POST /api/courses/{uuid}/enroll */
    @PostMapping("/{uuid}/enroll")
    public ResponseEntity<CourseDto.Response> enroll(
            @PathVariable UUID uuid,
            HttpServletRequest httpRequest) {

        Integer studentId = resolveStudentIdFromSession(httpRequest);
        CourseDto.Response course = courseService.findByUuid(uuid);
        Integer courseId = course.getId();
        
        enrollmentService.enrollOrReactivate(studentId, courseId);
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
    public ResponseEntity<List<CourseDto.Response>> getMyCourses(HttpServletRequest request) {
        Integer studentId = resolveStudentIdFromSession(request);
        return ResponseEntity.ok(courseService.findMyCourses(studentId));
    }

    /** GET /api/courses/{uuid}/content */
    @GetMapping("/{uuid}/content")
    public ResponseEntity<Map<String, String>> getCourseContent(
            @PathVariable UUID uuid,
            HttpServletRequest request) {

        Integer studentId = resolveStudentIdFromSession(request);
        CourseDto.Response course = courseService.findByUuid(uuid);

         // Check enrollment
        boolean enrolled = enrollmentService.isEnrolled(studentId, course.getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled to access course materials.");
        }
        Map<String, String> response = Map.of("material", course.getMaterial() != null ? course.getMaterial() : "");
        return ResponseEntity.ok(response);
    }

    /** PATCH /api/courses/{uuid} */
    @PatchMapping("/{uuid}")
    public ResponseEntity<CourseDto.Response> update(
            @PathVariable UUID uuid,
            @Valid @RequestBody CourseDto.UpdateRequest req,
            HttpServletRequest httpRequest) {
        Integer professorId = resolveProfessorIdFromSession(httpRequest);
        return ResponseEntity.ok(courseService.update(uuid, req, professorId));
    }

    /** DELETE /api/courses/{uuid} */
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid, HttpServletRequest httpRequest) {
        Integer professorId = resolveProfessorIdFromSession(httpRequest);
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

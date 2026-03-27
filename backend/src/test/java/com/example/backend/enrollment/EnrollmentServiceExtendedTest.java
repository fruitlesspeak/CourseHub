package com.example.backend.enrollment;

import com.example.backend.entity.Course;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.User;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.EnrollmentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceExtendedTest {

    private static final int STUDENT_ID  = 1;
    private static final int PROFESSOR_ID = 7;
    private static final int COURSE_ID   = 10;

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository     courseRepository;
    @Mock private UserRepository       userRepository;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, courseRepository, userRepository);
    }


    @Test
    void findByUserReturnsSingleActiveEnrollment() {
        Enrollment e = new Enrollment();
        e.setUserId(STUDENT_ID);
        e.setCourseId(COURSE_ID);
        e.setIsActive(true);

        when(enrollmentRepository.findByUserIdAndIsActiveTrue(STUDENT_ID)).thenReturn(List.of(e));

        List<Enrollment> result = enrollmentService.findByUser(STUDENT_ID);

        assertEquals(1, result.size());
        assertEquals(STUDENT_ID, result.get(0).getUserId());
        verify(enrollmentRepository).findByUserIdAndIsActiveTrue(STUDENT_ID);
    }

    @Test
    void findByUserReturnsMultipleActiveEnrollments() {
        Enrollment e1 = new Enrollment(); e1.setUserId(STUDENT_ID); e1.setCourseId(10); e1.setIsActive(true);
        Enrollment e2 = new Enrollment(); e2.setUserId(STUDENT_ID); e2.setCourseId(20); e2.setIsActive(true);

        when(enrollmentRepository.findByUserIdAndIsActiveTrue(STUDENT_ID)).thenReturn(List.of(e1, e2));

        assertEquals(2, enrollmentService.findByUser(STUDENT_ID).size());
    }

    @Test
    void findByUserReturnsEmptyListWhenNoActiveEnrollmentsExist() {
        when(enrollmentRepository.findByUserIdAndIsActiveTrue(STUDENT_ID)).thenReturn(List.of());

        assertTrue(enrollmentService.findByUser(STUDENT_ID).isEmpty());
    }

    @Test
    void findByCourseReturnsSingleActiveEnrollment() {
        Enrollment e = new Enrollment();
        e.setUserId(STUDENT_ID);
        e.setCourseId(COURSE_ID);
        e.setIsActive(true);

        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(COURSE_ID)).thenReturn(List.of(e));

        List<Enrollment> result = enrollmentService.findByCourse(COURSE_ID);

        assertEquals(1, result.size());
        assertEquals(COURSE_ID, result.get(0).getCourseId());
        verify(enrollmentRepository).findByCourseIdAndIsActiveTrue(COURSE_ID);
    }

    @Test
    void findByCourseReturnsEmptyListWhenNoneActive() {
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(COURSE_ID)).thenReturn(List.of());

        assertTrue(enrollmentService.findByCourse(COURSE_ID).isEmpty());
    }


    @Test
    void isEnrolledReturnsTrueWhenEnrollmentIsActive() {
        Enrollment e = new Enrollment();
        e.setIsActive(true);

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(e));

        assertTrue(enrollmentService.isEnrolled(STUDENT_ID, COURSE_ID));
    }

    @Test
    void isEnrolledReturnsFalseWhenEnrollmentIsInactive() {
        Enrollment e = new Enrollment();
        e.setIsActive(false);

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(e));

        assertFalse(enrollmentService.isEnrolled(STUDENT_ID, COURSE_ID));
    }

    @Test
    void isEnrolledReturnsFalseWhenNoEnrollmentExists() {
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertFalse(enrollmentService.isEnrolled(STUDENT_ID, COURSE_ID));
    }


    @Test
    void enrollOrReactivateThrowsEntityNotFoundWhenUserIsProfessor() {
        User professor = new User();
        professor.setId(PROFESSOR_ID);
        professor.setProfessor(true);

        when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.enrollOrReactivate(PROFESSOR_ID, COURSE_ID));

        assertEquals("No student found with id: " + PROFESSOR_ID, ex.getMessage());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void deactivateThrowsEntityNotFoundWhenUserIsProfessor() {
        User professor = new User();
        professor.setId(PROFESSOR_ID);
        professor.setProfessor(true);

        when(userRepository.findById(PROFESSOR_ID)).thenReturn(Optional.of(professor));

        assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.deactivate(PROFESSOR_ID, COURSE_ID));

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void newEnrollmentSavesCorrectUserIdCourseIdAndIsActiveTrue() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        Course course = new Course();
        course.setId(COURSE_ID);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.empty());
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());

        Enrollment saved = captor.getValue();
        assertEquals(STUDENT_ID, saved.getUserId());
        assertEquals(COURSE_ID,  saved.getCourseId());
        assertTrue(saved.getIsActive());
    }

    @Test
    void reactivationSetsIsActiveTrueAndSavesExistingRecord() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        Enrollment existing = new Enrollment();
        existing.setUserId(STUDENT_ID);
        existing.setCourseId(COURSE_ID);
        existing.setIsActive(false);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID);

        assertTrue(existing.getIsActive());
        verify(enrollmentRepository).save(existing);
        verify(courseRepository, never()).findById(any()); // Course existence check is SKIPPED on reactivation
    }

    @Test
    void deactivateSetsIsActiveFalseAndSavesRecord() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(STUDENT_ID);
        enrollment.setCourseId(COURSE_ID);
        enrollment.setIsActive(true);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        enrollmentService.deactivate(STUDENT_ID, COURSE_ID);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsActive());
    }

    @Test
    void deactivateThrowsEntityNotFoundWithCorrectMessageWhenEnrollmentMissing() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.deactivate(STUDENT_ID, COURSE_ID));

        assertEquals("Enrollment not found", ex.getMessage());
    }

    @Test
    void enrollOrReactivateThrowsEntityNotFoundWithCorrectMessageWhenStudentMissing() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));

        assertEquals("No student found with id: " + STUDENT_ID, ex.getMessage());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollOrReactivateThrowsEntityNotFoundWithCorrectMessageWhenCourseMissing() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.empty());
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));

        assertEquals("Course not found with id: " + COURSE_ID, ex.getMessage());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollOrReactivateThrowsIllegalStateWithCorrectMessageWhenAlreadyActive() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        Enrollment active = new Enrollment();
        active.setIsActive(true);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(active));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));

        assertEquals("User already enrolled in this course", ex.getMessage());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void deactivateThrowsIllegalStateWithCorrectMessageWhenAlreadyInactive() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        Enrollment inactive = new Enrollment();
        inactive.setIsActive(false);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(inactive));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> enrollmentService.deactivate(STUDENT_ID, COURSE_ID));

        assertEquals("Enrollment already inactive", ex.getMessage());
        verify(enrollmentRepository, never()).save(any());
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {

    private static final int STUDENT_ID = 1;
    private static final int COURSE_ID = 10;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(enrollmentRepository, courseRepository, userRepository);
    }

    @Test
    void mutation_enrollCreatesNewActiveEnrollmentWithStudentAndCourseIds() {
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setTitle("Test Course");

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(studentUser(STUDENT_ID)));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0, Enrollment.class));

        enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        Enrollment saved = captor.getValue();

        assertEquals(STUDENT_ID, saved.getUserId());
        assertEquals(COURSE_ID, saved.getCourseId());
        assertTrue(saved.getIsActive());
    }

    @Test
    void mutation_reactivateInactiveEnrollmentMarksItActiveAndSaves() {
        Enrollment existing = new Enrollment();
        existing.setUserId(STUDENT_ID);
        existing.setCourseId(COURSE_ID);
        existing.setIsActive(false);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(studentUser(STUDENT_ID)));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.save(any())).thenReturn(existing);

        enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID);

        assertTrue(existing.getIsActive());
        verify(enrollmentRepository).save(existing);
    }

    @Test
    void mutation_enrollWhenAlreadyActiveThrowsDuplicateErrorAndDoesNotSave() {
        Enrollment existing = new Enrollment();
        existing.setIsActive(true);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(studentUser(STUDENT_ID)));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));

        assertEquals("You are already enrolled in this course.", ex.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void mutation_deactivateActiveEnrollmentMarksItInactiveAndSaves() {
        Enrollment enrollment = new Enrollment();
        enrollment.setIsActive(true);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(studentUser(STUDENT_ID)));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any())).thenReturn(enrollment);

        enrollmentService.deactivate(STUDENT_ID, COURSE_ID);

        assertFalse(enrollment.getIsActive());
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void mutation_deactivateWhenAlreadyInactiveThrowsAndDoesNotSave() {
        Enrollment enrollment = new Enrollment();
        enrollment.setIsActive(false);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(studentUser(STUDENT_ID)));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> enrollmentService.deactivate(STUDENT_ID, COURSE_ID));

        assertEquals("You have already dropped this course.", ex.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void mutation_deactivateWhenEnrollmentMissingThrowsNotFound() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(studentUser(STUDENT_ID)));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.deactivate(STUDENT_ID, COURSE_ID));

        assertEquals("This enrollment could not be found.", ex.getMessage());
    }

    @Test
    void mutation_enrollWhenStudentMissingThrowsNotFound() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));

        assertEquals("This student account could not be found.", ex.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void mutation_enrollWhenProfessorIdPassedThrowsNoStudentFound() {
        User professor = new User();
        professor.setId(STUDENT_ID);
        professor.setProfessor(true);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(professor));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));

        assertEquals("This student account could not be found.", ex.getMessage());
        verify(courseRepository, never()).findById(COURSE_ID);
    }

    @Test
    void mutation_enrollWhenCourseMissingThrowsNotFound() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(studentUser(STUDENT_ID)));
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));

        assertEquals("This course could not be found.", ex.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void mutation_findByUserReturnsActiveEnrollments() {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(STUDENT_ID);
        enrollment.setCourseId(COURSE_ID);
        enrollment.setIsActive(true);
        when(enrollmentRepository.findByUserIdAndIsActiveTrue(STUDENT_ID)).thenReturn(List.of(enrollment));

        List<Enrollment> result = enrollmentService.findByUser(STUDENT_ID);

        assertEquals(1, result.size());
        assertEquals(COURSE_ID, result.get(0).getCourseId());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void mutation_findByCourseReturnsActiveEnrollments() {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(STUDENT_ID);
        enrollment.setCourseId(COURSE_ID);
        enrollment.setIsActive(true);
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(COURSE_ID)).thenReturn(List.of(enrollment));

        List<Enrollment> result = enrollmentService.findByCourse(COURSE_ID);

        assertEquals(1, result.size());
        assertEquals(STUDENT_ID, result.get(0).getUserId());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void mutation_isEnrolledReturnsTrueForActiveEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setIsActive(true);
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));

        assertTrue(enrollmentService.isEnrolled(STUDENT_ID, COURSE_ID));
    }

    @Test
    void mutation_isEnrolledReturnsFalseForInactiveEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setIsActive(false);
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.of(enrollment));

        assertFalse(enrollmentService.isEnrolled(STUDENT_ID, COURSE_ID));
    }

    @Test
    void mutation_isEnrolledReturnsFalseWhenEnrollmentMissing() {
        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(Optional.empty());

        assertFalse(enrollmentService.isEnrolled(STUDENT_ID, COURSE_ID));
    }

    private static User studentUser(int id) {
        User user = new User();
        user.setId(id);
        user.setProfessor(false);
        return user;
    }
}

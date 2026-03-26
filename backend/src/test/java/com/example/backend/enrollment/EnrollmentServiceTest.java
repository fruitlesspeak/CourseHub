package com.example.backend.enrollment;

import com.example.backend.dto.CourseDto;
import com.example.backend.dto.UserDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.User;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CourseService;
import com.example.backend.service.EnrollmentService;

import jakarta.persistence.EntityNotFoundException;

import com.example.backend.repository.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnrollmentServiceTest {
        
    private static final UUID COURSE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
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
    void shouldCreateNewEnrollment() {
        User user = new User();
        user.setId(STUDENT_ID);

        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(user));

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        when(enrollmentRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID);

        verify(enrollmentRepository).save(any());
    }

    @Test
    void shouldReactivateEnrollment() {
        User user = new User();
        user.setId(STUDENT_ID);

        Enrollment existing = new Enrollment();
        existing.setUserId(STUDENT_ID);
        existing.setCourseId(COURSE_ID);
        existing.setIsActive(false);

        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(user));

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));

        when(enrollmentRepository.save(any())).thenReturn(existing);

        enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID);

        assertTrue(existing.getIsActive());
        verify(enrollmentRepository).save(existing);
    }

    @Test
    void shouldFailIfAlreadyEnrolled() {
        User user = new User();
        user.setId(STUDENT_ID);

        Enrollment existing = new Enrollment();
        existing.setIsActive(true);

        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(user));

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> enrollmentService.enrollOrReactivate(STUDENT_ID, COURSE_ID));
    }


    @Test
    void shouldDeactivateEnrollment() {
        User user = new User();
        user.setId(STUDENT_ID);

        Enrollment enrollment = new Enrollment();
        enrollment.setIsActive(true);

        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(user));

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(enrollment));

        when(enrollmentRepository.save(any())).thenReturn(enrollment);

        enrollmentService.deactivate(STUDENT_ID, COURSE_ID);

        assertFalse(enrollment.getIsActive());
    }

    @Test
    void shouldFailIfAlreadyInactive() {
        User user = new User();
        user.setId(STUDENT_ID);

        Enrollment enrollment = new Enrollment();
        enrollment.setIsActive(false);

        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(user));

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.of(enrollment));

        assertThrows(IllegalStateException.class,
                () -> enrollmentService.deactivate(STUDENT_ID, COURSE_ID));
    }

    @Test
    void shouldThrowIfEnrollmentNotFound() {
        User user = new User();
        user.setId(STUDENT_ID);

        when(userRepository.findById(STUDENT_ID))
                .thenReturn(Optional.of(user));

        when(enrollmentRepository.findByUserIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> enrollmentService.deactivate(STUDENT_ID, COURSE_ID));
    }

}

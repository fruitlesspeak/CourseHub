package com.example.backend.importantdate;

import com.example.backend.dto.ImportantDateDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.ImportantDate;
import com.example.backend.entity.User;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.ImportantDateRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ImportantDateService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportantDateServiceTest {

    private static final int COURSE_ID = 10;
    private static final int OWNER_PROFESSOR_ID = 7;
    private static final int OTHER_PROFESSOR_ID = 9;
    private static final int IMPORTANT_DATE_ID = 5;

    @Mock
    private ImportantDateRepository importantDateRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private ImportantDateService importantDateService;

    @BeforeEach
    void setUp() {
        importantDateService = new ImportantDateService(importantDateRepository, courseRepository, userRepository);
    }

    @Test
    void createForOwnedCourseSavesNormalizedDescription() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseOwnedBy(OWNER_PROFESSOR_ID)));
        when(userRepository.findById(OWNER_PROFESSOR_ID)).thenReturn(Optional.of(professorUser(OWNER_PROFESSOR_ID)));
        when(importantDateRepository.save(any(ImportantDate.class))).thenAnswer(invocation -> {
            ImportantDate saved = invocation.getArgument(0, ImportantDate.class);
            saved.setId(IMPORTANT_DATE_ID);
            return saved;
        });

        ImportantDateDto.CreateRequest request = new ImportantDateDto.CreateRequest();
        request.setTitle("Midterm");
        request.setDescription("  Bring calculator and notes  ");
        request.setDueAt(OffsetDateTime.parse("2026-04-07T15:30:00Z"));

        ImportantDateDto.Response response = importantDateService.create(COURSE_ID, request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<ImportantDate> captor = ArgumentCaptor.forClass(ImportantDate.class);
        verify(importantDateRepository).save(captor.capture());
        ImportantDate saved = captor.getValue();

        assertEquals(COURSE_ID, saved.getCourseId());
        assertEquals(OWNER_PROFESSOR_ID, saved.getCreatedByUserId());
        assertEquals("Midterm", saved.getTitle());
        assertEquals("Bring calculator and notes", saved.getDescription());
        assertEquals(request.getDueAt(), saved.getDueAt());

        assertEquals(IMPORTANT_DATE_ID, response.getId());
        assertEquals("Bring calculator and notes", response.getDescription());
    }

    @Test
    void createForNonOwnerThrowsForbiddenAndDoesNotSave() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseOwnedBy(OWNER_PROFESSOR_ID)));

        CourseAccessDeniedException ex = assertThrows(
                CourseAccessDeniedException.class,
                () -> importantDateService.create(COURSE_ID, createRequest(), OTHER_PROFESSOR_ID)
        );

        assertEquals("You can only manage important dates for your own courses.", ex.getMessage());
        verifyNoInteractions(userRepository);
        verify(importantDateRepository, never()).save(any(ImportantDate.class));
    }

    @Test
    void createWithNonProfessorThrowsError() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseOwnedBy(OWNER_PROFESSOR_ID)));
        User student = new User();
        student.setId(OWNER_PROFESSOR_ID);
        student.setProfessor(false);
        when(userRepository.findById(OWNER_PROFESSOR_ID)).thenReturn(Optional.of(student));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> importantDateService.create(COURSE_ID, createRequest(), OWNER_PROFESSOR_ID)
        );

        assertEquals("No professor found with id: " + OWNER_PROFESSOR_ID, ex.getMessage());
        verify(importantDateRepository, never()).save(any(ImportantDate.class));
    }

    @Test
    void findByCourseInDateRangeWithEmptyCourseIdsReturnsEmptyList() {
        List<ImportantDateDto.Response> responses = importantDateService.findByCourseInDateRange(
                List.of(),
                OffsetDateTime.parse("2026-04-01T00:00:00Z"),
                OffsetDateTime.parse("2026-04-30T23:59:59Z")
        );

        assertTrue(responses.isEmpty());
        verifyNoInteractions(importantDateRepository);
    }

    @Test
    void findByCourseThrowsWhenCourseMissing() {
        when(courseRepository.existsById(COURSE_ID)).thenReturn(false);

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> importantDateService.findByCourse(COURSE_ID)
        );

        assertEquals("Course not found: " + COURSE_ID, ex.getMessage());
        verify(importantDateRepository, never()).findByCourseIdOrderByDueAtAsc(any());
    }

    @Test
    void updateOwnedImportantDateAppliesProvidedFields() {
        ImportantDate existing = existingImportantDate();
        when(importantDateRepository.findById(IMPORTANT_DATE_ID)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseOwnedBy(OWNER_PROFESSOR_ID)));
        when(importantDateRepository.save(any(ImportantDate.class))).thenAnswer(invocation -> invocation.getArgument(0, ImportantDate.class));

        ImportantDateDto.UpdateRequest request = new ImportantDateDto.UpdateRequest();
        request.setTitle("Updated title");
        request.setDescription("   ");
        request.setDueAt(OffsetDateTime.parse("2026-04-20T14:00:00Z"));

        ImportantDateDto.Response response = importantDateService.update(IMPORTANT_DATE_ID, request, OWNER_PROFESSOR_ID);

        assertEquals("Updated title", existing.getTitle());
        assertNull(existing.getDescription());
        assertEquals(request.getDueAt(), existing.getDueAt());
        assertEquals("Updated title", response.getTitle());
        assertNull(response.getDescription());
    }

    @Test
    void deleteAsNonOwnerThrowsForbiddenAndDoesNotDelete() {
        when(importantDateRepository.findById(IMPORTANT_DATE_ID)).thenReturn(Optional.of(existingImportantDate()));
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(courseOwnedBy(OWNER_PROFESSOR_ID)));

        CourseAccessDeniedException ex = assertThrows(
                CourseAccessDeniedException.class,
                () -> importantDateService.delete(IMPORTANT_DATE_ID, OTHER_PROFESSOR_ID)
        );

        assertEquals("You can only manage important dates for your own courses.", ex.getMessage());
        verify(importantDateRepository, never()).delete(any(ImportantDate.class));
    }

    private static ImportantDateDto.CreateRequest createRequest() {
        ImportantDateDto.CreateRequest request = new ImportantDateDto.CreateRequest();
        request.setTitle("Assignment");
        request.setDescription("Read chapter 3");
        request.setDueAt(OffsetDateTime.parse("2026-04-07T15:30:00Z"));
        return request;
    }

    private static Course courseOwnedBy(int professorId) {
        Course course = new Course();
        course.setId(COURSE_ID);
        course.setProfessorId(professorId);
        return course;
    }

    private static User professorUser(int id) {
        User user = new User();
        user.setId(id);
        user.setProfessor(true);
        return user;
    }

    private static ImportantDate existingImportantDate() {
        ImportantDate importantDate = new ImportantDate();
        importantDate.setId(IMPORTANT_DATE_ID);
        importantDate.setCourseId(COURSE_ID);
        importantDate.setCreatedByUserId(OWNER_PROFESSOR_ID);
        importantDate.setTitle("Original title");
        importantDate.setDescription("Original description");
        importantDate.setDueAt(OffsetDateTime.parse("2026-04-10T12:00:00Z"));
        return importantDate;
    }
}

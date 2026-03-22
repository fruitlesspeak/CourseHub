package com.example.backend.course;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.User;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CourseService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final UUID COURSE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int OWNER_PROFESSOR_ID = 7;
    private static final int OTHER_PROFESSOR_ID = 9;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, userRepository);
    }

    @Test
    void mutation_createWithHttpsLinkSavesLinkAsIs() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Databases");
        request.setCode("COMP4350");
        request.setDescription("  DB fundamentals  ");
        request.setTags(" sql,db ");
        request.setMaterial(" week 1 notes ");
        request.setDueDate(OffsetDateTime.parse("2026-04-05T12:30:00Z"));
        request.setLink("https://example.com/course");

        CourseDto.Response response = courseService.create(request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertEquals("https://example.com/course", saved.getLink());
        assertEquals("DB fundamentals", saved.getDescription());
        assertEquals("sql,db", saved.getTags());
        assertEquals("week 1 notes", saved.getMaterial());
        assertEquals(OffsetDateTime.parse("2026-04-05T12:30:00Z"), saved.getDueDate());
        assertEquals(OWNER_PROFESSOR_ID, saved.getProfessorId());

        assertEquals("https://example.com/course", response.getLink());
    }

    @Test
    void mutation_createWithWwwLinkNormalizesToHttps() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");
        request.setLink("www.example.com/course");

        CourseDto.Response response = courseService.create(request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertEquals("https://www.example.com/course", captor.getValue().getLink());
        assertEquals("https://www.example.com/course", response.getLink());
    }

    @Test
    void mutation_createWithHttpLinkThrowsValidationError() {
        when(userRepository.findById(OWNER_PROFESSOR_ID)).thenReturn(Optional.of(professorUser(OWNER_PROFESSOR_ID)));
        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");
        request.setLink("http://example.com/course");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.create(request, OWNER_PROFESSOR_ID)
        );

        assertEquals("Course link must start with https:// or www.", ex.getMessage());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void mutation_createWithMalformedLinkThrowsValidationError() {
        when(userRepository.findById(OWNER_PROFESSOR_ID)).thenReturn(Optional.of(professorUser(OWNER_PROFESSOR_ID)));
        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");
        request.setLink("https://");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.create(request, OWNER_PROFESSOR_ID)
        );

        assertEquals("Course link is invalid.", ex.getMessage());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void mutation_createWithBlankLinkStoresNull() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");
        request.setLink("   ");

        CourseDto.Response response = courseService.create(request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertNull(captor.getValue().getLink());
        assertNull(response.getLink());
    }

    @Test
    void mutation_createWithMissingProfessorThrowsError() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");
        request.setLink("https://example.com/course");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.create(request, 999)
        );

        assertEquals("No professor found with id: 999", ex.getMessage());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void mutation_createWithExistingNonProfessorUserThrowsError() {
        User student = new User();
        student.setId(OWNER_PROFESSOR_ID);
        student.setProfessor(false);
        when(userRepository.findById(OWNER_PROFESSOR_ID)).thenReturn(Optional.of(student));

        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.create(request, OWNER_PROFESSOR_ID)
        );

        assertEquals("No professor found with id: 7", ex.getMessage());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void mutation_createWithBlankOptionalTextFieldsStoresNulls() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");
        request.setDescription("   ");
        request.setTags("   ");
        request.setMaterial("   ");

        CourseDto.Response response = courseService.create(request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertNull(saved.getDescription());
        assertNull(saved.getTags());
        assertNull(saved.getMaterial());
        assertNull(response.getDescription());
        assertNull(response.getTags());
        assertNull(response.getMaterial());
    }

    @Test
    void mutation_createWithNullOptionalFieldsStoresNulls() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest request = new CourseDto.CreateRequest();
        request.setTitle("Intro to Testing");
        request.setCode("COMP200");
        request.setDescription(null);
        request.setTags(null);
        request.setMaterial(null);
        request.setLink(null);

        CourseDto.Response response = courseService.create(request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertNull(saved.getDescription());
        assertNull(saved.getTags());
        assertNull(saved.getMaterial());
        assertNull(saved.getLink());
        assertNull(response.getDescription());
        assertNull(response.getTags());
        assertNull(response.getMaterial());
        assertNull(response.getLink());
    }

    @Test
    void mutation_findAllReturnsMappedCourses() {
        when(courseRepository.findAll()).thenReturn(List.of(existingCourse(OWNER_PROFESSOR_ID)));

        List<CourseDto.Response> response = courseService.findAll();

        assertEquals(1, response.size());
        assertEquals(COURSE_UUID, response.get(0).getUuid());
        assertEquals("Databases", response.get(0).getTitle());
        assertEquals("COMP4350", response.get(0).getCode());
        assertEquals(OWNER_PROFESSOR_ID, response.get(0).getProfessorId());
    }

    @Test
    void mutation_findByUuidReturnsMappedCourse() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existingCourse(OWNER_PROFESSOR_ID)));

        CourseDto.Response response = courseService.findByUuid(COURSE_UUID);

        assertEquals(COURSE_UUID, response.getUuid());
        assertEquals("Databases", response.getTitle());
        assertEquals("COMP4350", response.getCode());
        assertEquals("https://example.com/old-course", response.getLink());
        assertEquals(OWNER_PROFESSOR_ID, response.getProfessorId());
    }

    @Test
    void mutation_findByUuidWhenCourseMissingThrowsNotFound() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.findByUuid(COURSE_UUID)
        );

        assertEquals("Course not found: " + COURSE_UUID, ex.getMessage());
    }

    @Test
    void mutation_findByProfessorReturnsMappedCourses() {
        when(courseRepository.findByProfessorId(OWNER_PROFESSOR_ID)).thenReturn(List.of(existingCourse(OWNER_PROFESSOR_ID)));

        List<CourseDto.Response> response = courseService.findByProfessor(OWNER_PROFESSOR_ID);

        assertEquals(1, response.size());
        assertEquals(COURSE_UUID, response.get(0).getUuid());
        assertEquals(OWNER_PROFESSOR_ID, response.get(0).getProfessorId());
    }

    @Test
    void mutation_searchReturnsMappedCourses() {
        when(courseRepository.findByTitleContainingIgnoreCase("Data")).thenReturn(List.of(existingCourse(OWNER_PROFESSOR_ID)));

        List<CourseDto.Response> response = courseService.search("Data");

        assertEquals(1, response.size());
        assertEquals(COURSE_UUID, response.get(0).getUuid());
        assertEquals("Databases", response.get(0).getTitle());
    }

    @Test
    void mutation_updateCourseAsOwnerUpdatesEditableFieldsAndReturnsResponse() {
        Course existing = existingCourse(OWNER_PROFESSOR_ID);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0, Course.class));

        OffsetDateTime dueDate = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        CourseDto.UpdateRequest request = new CourseDto.UpdateRequest();
        request.setDescription(" Updated description ");
        request.setTags(" db,sql ");
        request.setMaterial(" Week 4 slides ");
        request.setDueDate(dueDate);
        request.setLink("www.example.com/new-course");

        CourseDto.Response response = courseService.update(COURSE_UUID, request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertEquals("Updated description", saved.getDescription());
        assertEquals("db,sql", saved.getTags());
        assertEquals("Week 4 slides", saved.getMaterial());
        assertEquals(dueDate, saved.getDueDate());
        assertEquals("https://www.example.com/new-course", saved.getLink());
        assertEquals(OWNER_PROFESSOR_ID, saved.getProfessorId());

        assertEquals(COURSE_UUID, response.getUuid());
        assertEquals("Updated description", response.getDescription());
        assertEquals("db,sql", response.getTags());
        assertEquals("Week 4 slides", response.getMaterial());
        assertEquals(dueDate, response.getDueDate());
    }

    @Test
    void mutation_updateCourseAsOwnerUpdatesTitleAndCode() {
        Course existing = existingCourse(OWNER_PROFESSOR_ID);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0, Course.class));

        CourseDto.UpdateRequest request = new CourseDto.UpdateRequest();
        request.setTitle("Advanced Databases");
        request.setCode("COMP4950");

        CourseDto.Response response = courseService.update(COURSE_UUID, request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertEquals("Advanced Databases", saved.getTitle());
        assertEquals("COMP4950", saved.getCode());
        assertEquals("Advanced Databases", response.getTitle());
        assertEquals("COMP4950", response.getCode());
    }

    @Test
    void mutation_updateCourseAsOwnerBlankOptionalFieldsBecomeNull() {
        Course existing = existingCourse(OWNER_PROFESSOR_ID);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0, Course.class));

        CourseDto.UpdateRequest request = new CourseDto.UpdateRequest();
        request.setDescription("   ");
        request.setTags("   ");
        request.setMaterial("   ");
        request.setLink("   ");

        CourseDto.Response response = courseService.update(COURSE_UUID, request, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        Course saved = captor.getValue();

        assertNull(saved.getDescription());
        assertNull(saved.getTags());
        assertNull(saved.getMaterial());
        assertNull(saved.getLink());
        assertNull(response.getDescription());
        assertNull(response.getTags());
        assertNull(response.getMaterial());
        assertNull(response.getLink());
    }

    @Test
    void mutation_updateCourseAsNonOwnerThrowsForbiddenAndDoesNotSave() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existingCourse(OWNER_PROFESSOR_ID)));

        CourseAccessDeniedException ex = assertThrows(
                CourseAccessDeniedException.class,
                () -> courseService.update(COURSE_UUID, new CourseDto.UpdateRequest(), OTHER_PROFESSOR_ID)
        );

        assertEquals("You can only modify your own courses.", ex.getMessage());
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void mutation_deleteCourseAsOwnerDeletesCourse() {
        Course existing = existingCourse(OWNER_PROFESSOR_ID);
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));

        courseService.delete(COURSE_UUID, OWNER_PROFESSOR_ID);

        verify(courseRepository).delete(existing);
    }

    @Test
    void mutation_deleteCourseAsNonOwnerThrowsForbiddenAndDoesNotDelete() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existingCourse(OWNER_PROFESSOR_ID)));

        CourseAccessDeniedException ex = assertThrows(
                CourseAccessDeniedException.class,
                () -> courseService.delete(COURSE_UUID, OTHER_PROFESSOR_ID)
        );

        assertEquals("You can only modify your own courses.", ex.getMessage());
        verify(courseRepository, never()).delete(any(Course.class));
    }

    private void stubProfessorAndSave(int professorId) {
        when(userRepository.findById(professorId)).thenReturn(Optional.of(professorUser(professorId)));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0, Course.class);
            saved.setId(1);
            if (saved.getUuid() == null) {
                saved.setUuid(COURSE_UUID);
            }
            return saved;
        });
    }

    private static Course existingCourse(int professorId) {
        Course course = new Course();
        course.setId(1);
        course.setUuid(COURSE_UUID);
        course.setTitle("Databases");
        course.setCode("COMP4350");
        course.setDescription("Old description");
        course.setLink("https://example.com/old-course");
        course.setTags("old");
        course.setMaterial("old notes");
        course.setDueDate(OffsetDateTime.parse("2026-03-01T12:00:00Z"));
        course.setProfessorId(professorId);
        return course;
    }

    private static User professorUser(int id) {
        User user = new User();
        user.setId(id);
        user.setProfessor(true);
        return user;
    }
}

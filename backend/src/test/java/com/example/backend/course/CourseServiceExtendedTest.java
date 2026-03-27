package com.example.backend.course;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.User;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.EnrollmentRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceExtendedTest {

    private static final UUID  COURSE_UUID        = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int   OWNER_PROFESSOR_ID = 7;
    private static final int   OTHER_PROFESSOR_ID = 9;
    private static final int   STUDENT_ID         = 1;

    @Mock private CourseRepository     courseRepository;
    @Mock private UserRepository       userRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, userRepository, enrollmentRepository);
    }

    @Test
    void findAllReturnsMappedResponsesForEveryPersistedCourse() {
        Course c1 = buildCourse(1, "Java Basics",    "COMP101", OWNER_PROFESSOR_ID);
        Course c2 = buildCourse(2, "Data Structures", "COMP201", OTHER_PROFESSOR_ID);

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of());
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(2)).thenReturn(List.of());

        List<CourseDto.Response> result = courseService.findAll();

        assertEquals(2, result.size());
        assertEquals("Java Basics",     result.get(0).getTitle());
        assertEquals("Data Structures", result.get(1).getTitle());
    }

    @Test
    void findAllReturnsEmptyListWhenNoCoursesPersisted() {
        when(courseRepository.findAll()).thenReturn(List.of());

        assertTrue(courseService.findAll().isEmpty());
    }

    @Test
    void findByProfessorReturnsOnlyThatProfessorsCoursesAsMappedResponses() {
        Course c1 = buildCourse(1, "Databases", "COMP4350", OWNER_PROFESSOR_ID);
        Course c2 = buildCourse(2, "Algorithms", "COMP3430", OWNER_PROFESSOR_ID);

        when(courseRepository.findByProfessorId(OWNER_PROFESSOR_ID)).thenReturn(List.of(c1, c2));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of());
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(2)).thenReturn(List.of());

        List<CourseDto.Response> result = courseService.findByProfessor(OWNER_PROFESSOR_ID);

        assertEquals(2, result.size());
        result.forEach(r -> assertEquals(OWNER_PROFESSOR_ID, r.getProfessorId()));
    }

    @Test
    void findByProfessorReturnsEmptyListWhenProfessorHasNoCourses() {
        when(courseRepository.findByProfessorId(OWNER_PROFESSOR_ID)).thenReturn(List.of());

        assertTrue(courseService.findByProfessor(OWNER_PROFESSOR_ID).isEmpty());
    }


    @Test
    void searchReturnsCoursesThatContainKeywordCaseInsensitive() {
        Course c = buildCourse(1, "Introduction to Java", "COMP101", OWNER_PROFESSOR_ID);

        when(courseRepository.findByTitleContainingIgnoreCase("java")).thenReturn(List.of(c));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of());

        List<CourseDto.Response> result = courseService.search("java");

        assertEquals(1, result.size());
        assertEquals("Introduction to Java", result.get(0).getTitle());
        verify(courseRepository).findByTitleContainingIgnoreCase("java");
    }

    @Test
    void searchReturnsEmptyListWhenNoTitleMatches() {
        when(courseRepository.findByTitleContainingIgnoreCase("nonexistent")).thenReturn(List.of());

        assertTrue(courseService.search("nonexistent").isEmpty());
    }

    @Test
    void findByUuidThrowsEntityNotFoundWhenCourseDoesNotExist() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> courseService.findByUuid(COURSE_UUID));

        assertTrue(ex.getMessage().contains(COURSE_UUID.toString()));
    }

    @Test
    void ensureStudentEnrolledDoesNotThrowWhenActiveEnrollmentExists() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndIsActiveTrue(STUDENT_ID, 10))
                .thenReturn(true);

        assertDoesNotThrow(() -> courseService.ensureStudentEnrolled(STUDENT_ID, 10));
    }

    @Test
    void ensureStudentEnrolledThrowsCourseAccessDeniedWhenNotEnrolled() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndIsActiveTrue(STUDENT_ID, 10))
                .thenReturn(false);

        CourseAccessDeniedException ex = assertThrows(CourseAccessDeniedException.class,
                () -> courseService.ensureStudentEnrolled(STUDENT_ID, 10));

        assertEquals("Student is not enrolled in this course", ex.getMessage());
    }

    @Test
    void createWithNullLinkStoresNull() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest req = minimalRequest();
        req.setLink(null);

        courseService.create(req, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertNull(captor.getValue().getLink());
    }

    @Test
    void createWithHttpLinkThrowsWithCorrectMessage() {
        when(userRepository.findById(OWNER_PROFESSOR_ID))
                .thenReturn(Optional.of(professorUser(OWNER_PROFESSOR_ID)));

        CourseDto.CreateRequest req = minimalRequest();
        req.setLink("http://example.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.create(req, OWNER_PROFESSOR_ID));

        assertEquals("Course link must start with https:// or www.", ex.getMessage());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createWithLinkMissingHostThrowsInvalidLinkMessage() {
        when(userRepository.findById(OWNER_PROFESSOR_ID))
                .thenReturn(Optional.of(professorUser(OWNER_PROFESSOR_ID)));

        CourseDto.CreateRequest req = minimalRequest();
        req.setLink("https://");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.create(req, OWNER_PROFESSOR_ID));

        assertEquals("Course link is invalid.", ex.getMessage());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createWithWwwLinkPrefixesHttps() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest req = minimalRequest();
        req.setLink("www.example.com/course");

        courseService.create(req, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertEquals("https://www.example.com/course", captor.getValue().getLink());
    }

    @Test
    void createWithWhitespaceOnlyDescriptionStoresNull() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest req = minimalRequest();
        req.setDescription("   ");

        courseService.create(req, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertNull(captor.getValue().getDescription());
    }

    @Test
    void createWithWhitespaceOnlyTagsStoresNull() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest req = minimalRequest();
        req.setTags("   ");

        courseService.create(req, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertNull(captor.getValue().getTags());
    }

    @Test
    void createWithWhitespaceOnlyMaterialStoresNull() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest req = minimalRequest();
        req.setMaterial("   ");

        courseService.create(req, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertNull(captor.getValue().getMaterial());
    }

    @Test
    void createTrimsDescriptionTagsAndMaterial() {
        stubProfessorAndSave(OWNER_PROFESSOR_ID);

        CourseDto.CreateRequest req = minimalRequest();
        req.setDescription("  Core concepts  ");
        req.setTags("  java, spring  ");
        req.setMaterial("  Week 1  ");

        courseService.create(req, OWNER_PROFESSOR_ID);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertEquals("Core concepts", captor.getValue().getDescription());
        assertEquals("java, spring",  captor.getValue().getTags());
        assertEquals("Week 1",        captor.getValue().getMaterial());
    }

    @Test
    void updateDoesNotOverwriteFieldsWhenRequestValuesAreNull() {
        Course existing = buildCourse(1, "Old Title", "OLD101", OWNER_PROFESSOR_ID);
        existing.setUuid(COURSE_UUID);
        existing.setDescription("Old description");
        existing.setTags("old");
        existing.setMaterial("old notes");

        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CourseDto.UpdateRequest req = new CourseDto.UpdateRequest(); // All fields null, nothing should change

        CourseDto.Response response = courseService.update(COURSE_UUID, req, OWNER_PROFESSOR_ID);

        assertEquals("Old Title",       response.getTitle());
        assertEquals("OLD101",          response.getCode());
        assertEquals("Old description", response.getDescription());
        assertEquals("old",             response.getTags());
        assertEquals("old notes",       response.getMaterial());
    }

    @Test
    void updateOnlyChangesFieldsThatAreProvided() {
        Course existing = buildCourse(1, "Old Title", "OLD101", OWNER_PROFESSOR_ID);
        existing.setUuid(COURSE_UUID);
        existing.setDescription("Old description");

        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of());

        CourseDto.UpdateRequest req = new CourseDto.UpdateRequest();
        req.setDescription("New description");  // only this changes

        CourseDto.Response response = courseService.update(COURSE_UUID, req, OWNER_PROFESSOR_ID);

        assertEquals("Old Title",       response.getTitle());       // unchanged
        assertEquals("OLD101",          response.getCode());        // unchanged
        assertEquals("New description", response.getDescription()); // changed
    }

    @Test
    void createThrowsIllegalArgumentWhenUserExistsButIsNotProfessor() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setProfessor(false);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.create(minimalRequest(), STUDENT_ID));

        assertEquals("No professor found with id: " + STUDENT_ID, ex.getMessage());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createThrowsIllegalArgumentWithCorrectMessageWhenProfessorIdNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> courseService.create(minimalRequest(), 999));

        assertEquals("No professor found with id: 999", ex.getMessage());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void deleteThrowsEntityNotFoundWhenCourseUuidDoesNotExist() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> courseService.delete(COURSE_UUID, OWNER_PROFESSOR_ID));

        verify(courseRepository, never()).delete(any());
    }

    @Test
    void updateThrowsEntityNotFoundWhenCourseUuidDoesNotExist() {
        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> courseService.update(COURSE_UUID, new CourseDto.UpdateRequest(), OWNER_PROFESSOR_ID));

        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateThrowsCourseAccessDeniedWithCorrectMessageForNonOwner() {
        Course existing = buildCourse(1, "Test", "T101", OWNER_PROFESSOR_ID);
        existing.setUuid(COURSE_UUID);

        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));

        CourseAccessDeniedException ex = assertThrows(CourseAccessDeniedException.class,
                () -> courseService.update(COURSE_UUID, new CourseDto.UpdateRequest(), OTHER_PROFESSOR_ID));

        assertEquals("You can only modify your own courses.", ex.getMessage());
    }

    @Test
    void deleteThrowsCourseAccessDeniedWithCorrectMessageForNonOwner() {
        Course existing = buildCourse(1, "Test", "T101", OWNER_PROFESSOR_ID);
        existing.setUuid(COURSE_UUID);

        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(existing));

        CourseAccessDeniedException ex = assertThrows(CourseAccessDeniedException.class,
                () -> courseService.delete(COURSE_UUID, OTHER_PROFESSOR_ID));

        assertEquals("You can only modify your own courses.", ex.getMessage());
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void findByUuidReturnsEnrolledCountMatchingStudentListSize() {
        Course course = buildCourse(1, "Test", "T101", OWNER_PROFESSOR_ID);
        course.setUuid(COURSE_UUID);

        Enrollment e1 = new Enrollment(); e1.setUserId(10); e1.setCourseId(1); e1.setIsActive(true);
        Enrollment e2 = new Enrollment(); e2.setUserId(11); e2.setCourseId(1); e2.setIsActive(true);

        User u1 = buildStudent(10, "Alice", "Smith");
        User u2 = buildStudent(11, "Bob",   "Jones");

        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of(e1, e2));
        when(userRepository.findById(10)).thenReturn(Optional.of(u1));
        when(userRepository.findById(11)).thenReturn(Optional.of(u2));

        CourseDto.Response response = courseService.findByUuid(COURSE_UUID);

        assertEquals(2, response.getEnrolledCount());
        assertEquals(2, response.getStudents().size());
    }

    @Test
    void findByUuidThrowsEntityNotFoundWhenEnrolledUserIsMissing() {
        Course course = buildCourse(1, "Test", "T101", OWNER_PROFESSOR_ID);
        course.setUuid(COURSE_UUID);

        Enrollment e = new Enrollment(); e.setUserId(99); e.setCourseId(1); e.setIsActive(true);

        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of(e));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> courseService.findByUuid(COURSE_UUID));
    }

    private void stubProfessorAndSave(int professorId) {
        when(userRepository.findById(professorId)).thenReturn(Optional.of(professorUser(professorId)));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(1);
            if (c.getUuid() == null) c.setUuid(COURSE_UUID);
            return c;
        });
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of());
    }

    private static CourseDto.CreateRequest minimalRequest() {
        CourseDto.CreateRequest req = new CourseDto.CreateRequest();
        req.setTitle("Test Course");
        req.setCode("TEST101");
        return req;
    }

    private static Course buildCourse(int id, String title, String code, int professorId) {
        Course c = new Course();
        c.setId(id);
        c.setUuid(UUID.randomUUID());
        c.setTitle(title);
        c.setCode(code);
        c.setProfessorId(professorId);
        c.setDueDate(OffsetDateTime.parse("2026-06-01T00:00:00Z"));
        return c;
    }

    private static User professorUser(int id) {
        User u = new User();
        u.setId(id);
        u.setProfessor(true);
        return u;
    }

    private static User buildStudent(int id, String firstName, String lastName) {
        User u = new User();
        u.setId(id);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setProfessor(false);
        return u;
    }
}

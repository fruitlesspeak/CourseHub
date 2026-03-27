package com.example.backend.course;

import com.example.backend.dto.CourseDto;
import com.example.backend.entity.Course;
import com.example.backend.entity.Enrollment;
import com.example.backend.entity.User;
import com.example.backend.exception.CourseAccessDeniedException;
import com.example.backend.repository.CourseRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CourseService;
import com.example.backend.repository.EnrollmentRepository;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final UUID COURSE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final int OWNER_PROFESSOR_ID = 7;
    private static final int OTHER_PROFESSOR_ID = 9;
    private static final int STUDENT_ID = 1;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, userRepository, enrollmentRepository);
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

    @Test
    void mutation_findByTagReturnsMatchingCourses() {
        Course course = existingCourse(OWNER_PROFESSOR_ID);
        course.setTags("java,spring");

        when(courseRepository.findByTagsContainingIgnoreCase("java"))
                .thenReturn(List.of(course));

        List<CourseDto.Response> result = courseService.findByTag("java");

        assertEquals(1, result.size());
        assertEquals("java,spring", result.get(0).getTags());
    }


    @Test
    void mutation_findMyCoursesReturnsActiveEnrolledCourses() {
        int studentId = 5;

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(studentId);
        enrollment.setCourseId(1);
        enrollment.setIsActive(true);

        Course course = existingCourse(OWNER_PROFESSOR_ID);
        course.setId(1);

        when(enrollmentRepository.findByUserIdAndIsActiveTrue(studentId))
                .thenReturn(List.of(enrollment));

        when(courseRepository.findByIdIn(List.of(1)))
                .thenReturn(List.of(course));

        List<CourseDto.Response> result = courseService.findMyCourses(studentId);

        assertEquals(1, result.size());
        assertEquals(course.getUuid(), result.get(0).getUuid());
    }

    @Test
    void mutation_findMyCoursesWithNoEnrollmentsReturnsEmptyList() {
        int studentId = 5;

        when(enrollmentRepository.findByUserIdAndIsActiveTrue(studentId))
                .thenReturn(List.of());


        List<CourseDto.Response> result = courseService.findMyCourses(studentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void mutation_findMyCoursesWithMissingCoursesStillReturnsEmptySafely() {
        int studentId = 5;

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(studentId);
        enrollment.setCourseId(999); // doesn't exist

        when(enrollmentRepository.findByUserIdAndIsActiveTrue(studentId))
                .thenReturn(List.of(enrollment));

        when(courseRepository.findByIdIn(List.of(999)))
                .thenReturn(List.of());

        List<CourseDto.Response> result = courseService.findMyCourses(studentId);

        assertTrue(result.isEmpty());
    }

    @Test
    void mutation_findByUuidIncludesStudentsInCourseResponse() {
        UUID uuid = UUID.randomUUID();

        Course course = new Course();
        course.setId(1);
        course.setUuid(uuid);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(10);
        enrollment.setCourseId(1);
        enrollment.setIsActive(true);

        User user = new User();
        user.setId(10);
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        when(courseRepository.findByUuid(uuid))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1))
                .thenReturn(List.of(enrollment));

        when(userRepository.findById(10))
                .thenReturn(Optional.of(user));

        CourseDto.Response response = courseService.findByUuid(uuid);

        assertEquals(1, response.getStudents().size());
        assertEquals(1, response.getEnrolledCount());
        assertEquals("John", response.getStudents().get(0).getFirstName());
    }

    @Test
    void mutation_findByUuidWithMissingStudentInEnrollmentThrowsNotFound() {
        Course course = new Course();
        course.setId(1);
        course.setUuid(COURSE_UUID);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(STUDENT_ID);
        enrollment.setCourseId(1);
        enrollment.setIsActive(true);

        when(courseRepository.findByUuid(COURSE_UUID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of(enrollment));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.findByUuid(COURSE_UUID)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void mutation_findByUuidReflectsEnrollmentInCourseStudents() {
        UUID uuid = COURSE_UUID;

        Course course = new Course();
        course.setId(1);
        course.setUuid(uuid);

        User user = new User();
        user.setId(STUDENT_ID);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("test@test.com");

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(STUDENT_ID);
        enrollment.setCourseId(1);
        enrollment.setIsActive(true);        

        when(courseRepository.findByUuid(uuid)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1)).thenReturn(List.of(enrollment));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(user));

        CourseDto.Response response = courseService.findByUuid(uuid);

        assertNotNull(response.getStudents());
        assertEquals(1, response.getStudents().size());
        assertEquals(STUDENT_ID, response.getStudents().get(0).getId());
        assertEquals(1, response.getEnrolledCount());
    }

    @Test
    void mutation_findByUuidWithNoEnrollmentsReturnsEmptyStudentsList() {
        UUID uuid = COURSE_UUID;

        Course course = new Course();
        course.setId(1);
        course.setUuid(uuid);

        when(courseRepository.findByUuid(uuid))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1))
                .thenReturn(List.of());

        CourseDto.Response response = courseService.findByUuid(uuid);

        assertNotNull(response.getStudents());
        assertTrue(response.getStudents().isEmpty());
        assertEquals(0, response.getEnrolledCount());
    }

    @Test
    void mutation_findByUuidWithMultipleStudentsReturnsAllStudents() {
        UUID uuid = COURSE_UUID;

        Course course = new Course();
        course.setId(1);
        course.setUuid(uuid);

        Enrollment e1 = new Enrollment();
        e1.setUserId(1);
        e1.setCourseId(1);
        e1.setIsActive(true);

        Enrollment e2 = new Enrollment();
        e2.setUserId(2);
        e2.setCourseId(1);
        e2.setIsActive(true);

        User u1 = new User();
        u1.setId(1);
        u1.setFirstName("A");

        User u2 = new User();
        u2.setId(2);
        u2.setFirstName("B");

        when(courseRepository.findByUuid(uuid))
                .thenReturn(Optional.of(course));

        when(enrollmentRepository.findByCourseIdAndIsActiveTrue(1))
                .thenReturn(List.of(e1, e2));

        when(userRepository.findById(1)).thenReturn(Optional.of(u1));
        when(userRepository.findById(2)).thenReturn(Optional.of(u2));

        CourseDto.Response response = courseService.findByUuid(uuid);

        assertEquals(2, response.getStudents().size());
        assertEquals(2, response.getEnrolledCount());
    }

    @Test
    void mutation_ensureStudentEnrolledAllowsActiveEnrollment() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndIsActiveTrue(STUDENT_ID, 1)).thenReturn(true);

        assertDoesNotThrow(() -> courseService.ensureStudentEnrolled(STUDENT_ID, 1));
    }

    @Test
    void mutation_ensureStudentEnrolledRejectsMissingEnrollment() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndIsActiveTrue(STUDENT_ID, 1)).thenReturn(false);

        CourseAccessDeniedException ex = assertThrows(
                CourseAccessDeniedException.class,
                () -> courseService.ensureStudentEnrolled(STUDENT_ID, 1)
        );

        assertEquals("Student is not enrolled in this course", ex.getMessage());
    }
}

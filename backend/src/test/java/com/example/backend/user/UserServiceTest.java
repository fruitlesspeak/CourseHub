package com.example.backend.user;

import com.example.backend.dto.UserDto;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UUID USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void createWithValidRequestHashesPasswordAndReturnsResponse() {
        when(userRepository.existsByEmail("student@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0, User.class);
            saved.setId(42);
            saved.setUuid(USER_UUID);
            return saved;
        });

        UserDto.CreateRequest request = new UserDto.CreateRequest();
        request.setEmail("student@test.com");
        request.setPassword("Password123!");
        request.setFirstName("Student");
        request.setLastName("User");
        request.setStudentId("S12345");

        UserDto.Response response = userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("student@test.com", saved.getEmail());
        assertNotEquals("Password123!", saved.getPasswordHash());
        assertTrue(new BCryptPasswordEncoder().matches("Password123!", saved.getPasswordHash()));
        assertEquals("Student", saved.getFirstName());
        assertEquals("User", saved.getLastName());
        assertEquals("S12345", saved.getStudentId());
        assertEquals(42, response.getId());
        assertEquals(USER_UUID, response.getUuid());
    }

    @Test
    void createWithDuplicateEmailThrowsError() {
        UserDto.CreateRequest request = new UserDto.CreateRequest();
        request.setEmail("taken@test.com");
        request.setPassword("Password123!");
        request.setFirstName("Taken");
        request.setLastName("User");
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(request));

        assertEquals("An account with this email already exists.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createProfessorWithStudentIdThrowsError() {
        UserDto.CreateRequest request = new UserDto.CreateRequest();
        request.setEmail("prof@test.com");
        request.setPassword("Password123!");
        request.setFirstName("Prof");
        request.setLastName("User");
        request.setProfessor(true);
        request.setStudentId("S99999");
        when(userRepository.existsByEmail("prof@test.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.create(request));

        assertEquals("A professor account cannot have a student ID.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateWithNewEmailAndProfileFieldsPersistsChanges() {
        User existing = existingStudent();
        when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("updated@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setEmail("updated@test.com");
        request.setFirstName("Updated");
        request.setLastName("Name");

        UserDto.Response response = userService.update(USER_UUID, request);

        assertEquals("updated@test.com", existing.getEmail());
        assertEquals("Updated", existing.getFirstName());
        assertEquals("Name", existing.getLastName());
        assertEquals("updated@test.com", response.getEmail());
        assertEquals("Updated", response.getFirstName());
    }

    @Test
    void updateWithDuplicateEmailThrowsError() {
        when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.of(existingStudent()));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setEmail("taken@test.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.update(USER_UUID, request));

        assertEquals("An account with this email already exists.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateToProfessorWithStudentIdThrowsError() {
        when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.of(existingStudent()));

        UserDto.UpdateRequest request = new UserDto.UpdateRequest();
        request.setIsProfessor(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.update(USER_UUID, request));

        assertEquals("A professor account cannot have a student ID.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findProfessorsReturnsOnlyProfessorResponses() {
        when(userRepository.findByIsProfessor(true)).thenReturn(List.of(professorUser()));

        List<UserDto.Response> responses = userService.findProfessors();

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).isProfessor());
        assertEquals("prof@test.com", responses.get(0).getEmail());
    }

    @Test
    void deleteByUuidDeletesResolvedUser() {
        User existing = existingStudent();
        when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.of(existing));

        userService.delete(USER_UUID);

        verify(userRepository).delete(existing);
    }

    @Test
    void findByUuidWhenMissingThrowsEntityNotFound() {
        when(userRepository.findByUuid(USER_UUID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> userService.findByUuid(USER_UUID));

        assertEquals("This user could not be found.", ex.getMessage());
    }

    private static User existingStudent() {
        User user = new User();
        user.setId(21);
        user.setUuid(USER_UUID);
        user.setEmail("student@test.com");
        user.setPasswordHash("hashed");
        user.setFirstName("Student");
        user.setLastName("User");
        user.setProfessor(false);
        user.setStudentId("S12345");
        return user;
    }

    private static User professorUser() {
        User user = new User();
        user.setId(11);
        user.setUuid(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        user.setEmail("prof@test.com");
        user.setFirstName("Prof");
        user.setLastName("User");
        user.setProfessor(true);
        return user;
    }
}

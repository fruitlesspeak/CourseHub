package com.example.backend.register;

import com.example.backend.dto.auth.RegisterRequest;
import com.example.backend.dto.auth.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.entity.UserRole;
import com.example.backend.exception.EmailAlreadyInUseException;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.RegisterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterService(userRepository, passwordEncoder);
    }

    @Test
    void mutation_registerWithValidPayloadSavesUserAndReturnsResponse() {
        RegisterRequest request = new RegisterRequest(
                "  Demo  ",
                "  Student  ",
                "  DEMO@student.coursehub  ",
                "CourseHub123!",
                UserRole.STUDENT
        );

        when(userRepository.existsByEmail("demo@student.coursehub")).thenReturn(false);
        when(passwordEncoder.encode("CourseHub123!")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0, User.class);
            saved.setId(42);
            return saved;
        });

        UserResponse response = registerService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("Demo", savedUser.getFirstName());
        assertEquals("Student", savedUser.getLastName());
        assertEquals("demo@student.coursehub", savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPasswordHash());
        assertEquals(false, savedUser.isProfessor());

        assertEquals(42, response.id());
        assertEquals("Demo", response.firstName());
        assertEquals("Student", response.lastName());
        assertEquals("demo@student.coursehub", response.email());
        assertEquals(UserRole.STUDENT, response.role());

        verify(userRepository).existsByEmail("demo@student.coursehub");
        verify(passwordEncoder).encode("CourseHub123!");
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    void mutation_registerWithExistingEmailThrowsConflictException() {
        RegisterRequest request = new RegisterRequest(
                "Demo",
                "Student",
                "demo@student.coursehub",
                "CourseHub123!",
                UserRole.STUDENT
        );
        when(userRepository.existsByEmail("demo@student.coursehub")).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class, () -> registerService.register(request));

        verify(userRepository).existsByEmail("demo@student.coursehub");
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void mutation_registerWithProfessorRoleSavesProfessorAndReturnsProfessorResponse() {
        RegisterRequest request = new RegisterRequest(
                "  Ada  ",
                "  Lovelace  ",
                "  ADA@coursehub.test  ",
                "CourseHub123!",
                UserRole.PROFESSOR
        );

        when(userRepository.existsByEmail("ada@coursehub.test")).thenReturn(false);
        when(passwordEncoder.encode("CourseHub123!")).thenReturn("hashed-professor-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0, User.class);
            saved.setId(99);
            return saved;
        });

        UserResponse response = registerService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("Ada", savedUser.getFirstName());
        assertEquals("Lovelace", savedUser.getLastName());
        assertEquals("ada@coursehub.test", savedUser.getEmail());
        assertEquals("hashed-professor-password", savedUser.getPasswordHash());
        assertEquals(true, savedUser.isProfessor());
        assertNull(savedUser.getStudentId());

        assertEquals(99, response.id());
        assertEquals("Ada", response.firstName());
        assertEquals("Lovelace", response.lastName());
        assertEquals("ada@coursehub.test", response.email());
        assertEquals(UserRole.PROFESSOR, response.role());

        verify(userRepository).existsByEmail("ada@coursehub.test");
        verify(passwordEncoder).encode("CourseHub123!");
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }
}

package com.student_manager.feature.profile;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.auth.UserRepository;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.feature.teacher.TeacherRepository;
import com.student_manager.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileServiceImpl service;

    private User student;
    private Student profile;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setUsername("ada");
        student.setEmail("ada@example.com");
        student.setPasswordHash("hash");
        student.setRole(Role.STUDENT);

        profile = new Student();
        profile.setId(7L);
        profile.setFirstName("Ada");
        profile.setLastName("Lovelace");
        profile.setEmail("ada@example.com");
        profile.setMatriculationNumber("M-1");
    }

    private UpdateProfileRequest request(String username, String email) {
        UpdateProfileRequest r = new UpdateProfileRequest();
        r.setUsername(username);
        r.setEmail(email);
        r.setFirstName("Augusta");
        r.setLastName("King");
        return r;
    }

    @Test
    void updateWritesAccountAndLinkedProfileAndKeepsTheEmailsInSync() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(student));
        when(studentRepository.findByAccountUsername("ada")).thenReturn(Optional.empty());
        when(studentRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(profile));
        when(userRepository.existsByUsername("augusta")).thenReturn(false);
        when(userRepository.existsByEmail("augusta@example.com")).thenReturn(false);
        when(studentRepository.existsByEmailAndIdNot("augusta@example.com", 7L)).thenReturn(false);

        ProfileDTO result = service.update("ada", request("augusta", "augusta@example.com"));

        assertThat(result.getUsername()).isEqualTo("augusta");
        assertThat(result.getFirstName()).isEqualTo("Augusta");
        assertThat(profile.getEmail()).isEqualTo("augusta@example.com");
        assertThat(profile.getAccount()).isSameAs(student);
        assertThat(result.getMatriculationNumber()).isEqualTo("M-1");
    }

    @Test
    void updateRejectsAUsernameThatIsTaken() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(student));
        when(studentRepository.findByAccountUsername("ada")).thenReturn(Optional.of(profile));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.update("ada", request("taken", "ada@example.com")))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRejectsABlankNameWhenAProfileIsLinked() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(student));
        when(studentRepository.findByAccountUsername("ada")).thenReturn(Optional.of(profile));
        UpdateProfileRequest r = request("ada", "ada@example.com");
        r.setFirstName(" ");

        assertThatThrownBy(() -> service.update("ada", r)).isInstanceOf(ValidationException.class);
    }

    @Test
    void anAdminWithoutAProfileCanStillChangeUsernameAndEmail() {
        User admin = new User();
        admin.setUsername("root");
        admin.setEmail("root@example.com");
        admin.setRole(Role.ADMIN);
        when(userRepository.findByUsername("root")).thenReturn(Optional.of(admin));
        UpdateProfileRequest r = new UpdateProfileRequest();
        r.setUsername("root2");
        r.setEmail("root2@example.com");
        r.setFirstName(" Root ");

        ProfileDTO result = service.update("root", r);

        assertThat(result.getUsername()).isEqualTo("root2");
        assertThat(result.getFirstName()).isEqualTo("Root");
        assertThat(result.getLastName()).isNull();
        verifyNoInteractions(studentRepository, teacherRepository);
    }

    @Test
    void changePasswordRejectsAWrongCurrentPassword() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword("ada", new ChangePasswordRequest("wrong", "Password123")))
                .isInstanceOf(ValidationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordStoresTheNewHash() {
        when(userRepository.findByUsername("ada")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.encode("Password123")).thenReturn("newhash");

        service.changePassword("ada", new ChangePasswordRequest("old", "Password123"));

        assertThat(student.getPasswordHash()).isEqualTo("newhash");
    }
}

package com.student_manager.feature.student;

import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock private StudentRepository repository;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student existing;

    @BeforeEach
    void setUp() {
        existing = new Student();
        existing.setId(1L);
        existing.setFirstName("Ada");
        existing.setLastName("Lovelace");
        existing.setEmail("ada@example.com");
        existing.setMatriculationNumber("M-1");
    }

    private CreateStudentRequest requestWith(String email, String matriculationNumber) {
        CreateStudentRequest request = new CreateStudentRequest();
        request.setFirstName("Grace");
        request.setLastName("Hopper");
        request.setEmail(email);
        request.setMatriculationNumber(matriculationNumber);
        return request;
    }

    // ── create ──────────────────────────────────────────────────────

    @Test
    void createRejectsADuplicateEmail() {
        CreateStudentRequest request = requestWith("ada@example.com", "M-2");
        when(repository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already exists");

        verify(repository, never()).save(any());
    }

    // ── update ──────────────────────────────────────────────────────

    @Test
    void updateSucceedsWhenFieldsDoNotCollide() {
        CreateStudentRequest request = requestWith("grace@example.com", "M-2");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("grace@example.com", 1L)).thenReturn(false);
        when(repository.existsByMatriculationNumberAndIdNot("M-2", 1L)).thenReturn(false);
        when(repository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentDTO result = studentService.update(1L, request);

        assertThat(result.getEmail()).isEqualTo("grace@example.com");
        assertThat(result.getMatriculationNumber()).isEqualTo("M-2");
    }

    @Test
    void updateRejectsAnEmailAlreadyUsedByAnotherStudent() {
        CreateStudentRequest request = requestWith("other@example.com", "M-1");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("other@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> studentService.update(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already exists: other@example.com");

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsAMatriculationNumberAlreadyUsedByAnotherStudent() {
        CreateStudentRequest request = requestWith("ada@example.com", "M-99");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("ada@example.com", 1L)).thenReturn(false);
        when(repository.existsByMatriculationNumberAndIdNot("M-99", 1L)).thenReturn(true);

        assertThatThrownBy(() -> studentService.update(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Matriculation number already exists: M-99");

        verify(repository, never()).save(any());
    }

    @Test
    void updateThrowsWhenStudentDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.update(404L, requestWith("x@example.com", "M-x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ──────────────────────────────────────────────────────

    @Test
    void deleteThrowsWhenStudentDoesNotExist() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}

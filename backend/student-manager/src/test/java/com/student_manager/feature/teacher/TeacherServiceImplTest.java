package com.student_manager.feature.teacher;

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
class TeacherServiceImplTest {

    @Mock private TeacherRepository repository;

    @InjectMocks
    private TeacherServiceImpl teacherService;

    private Teacher existing;

    @BeforeEach
    void setUp() {
        existing = new Teacher();
        existing.setId(1L);
        existing.setFirstName("Ada");
        existing.setLastName("Lovelace");
        existing.setEmail("ada@example.com");
        existing.setDepartment("Computer Science");
    }

    private CreateTeacherRequest requestWith(String email) {
        CreateTeacherRequest request = new CreateTeacherRequest();
        request.setFirstName("Grace");
        request.setLastName("Hopper");
        request.setEmail(email);
        request.setDepartment("Mathematics");
        return request;
    }

    // ── create ──────────────────────────────────────────────────────

    @Test
    void createRejectsADuplicateEmail() {
        CreateTeacherRequest request = requestWith("ada@example.com");
        when(repository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> teacherService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already exists");

        verify(repository, never()).save(any());
    }

    @Test
    void createSucceedsWhenEmailIsUnique() {
        CreateTeacherRequest request = requestWith("grace@example.com");
        when(repository.existsByEmail("grace@example.com")).thenReturn(false);
        when(repository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherDTO result = teacherService.create(request);

        assertThat(result.getEmail()).isEqualTo("grace@example.com");
        assertThat(result.getDepartment()).isEqualTo("Mathematics");
    }

    // ── update ──────────────────────────────────────────────────────

    @Test
    void updateSucceedsWhenEmailDoesNotCollide() {
        CreateTeacherRequest request = requestWith("grace@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("grace@example.com", 1L)).thenReturn(false);
        when(repository.saveAndFlush(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherDTO result = teacherService.update(1L, request);

        assertThat(result.getEmail()).isEqualTo("grace@example.com");
    }

    @Test
    void updateRejectsAnEmailAlreadyUsedByAnotherTeacher() {
        CreateTeacherRequest request = requestWith("other@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByEmailAndIdNot("other@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> teacherService.update(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already exists: other@example.com");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateThrowsWhenTeacherDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.update(404L, requestWith("x@example.com")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── findById / findAll (inherited from CrudServiceSupport) ────────

    @Test
    void findByIdThrowsWhenTeacherDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ──────────────────────────────────────────────────────

    @Test
    void deleteThrowsWhenTeacherDoesNotExist() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> teacherService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}

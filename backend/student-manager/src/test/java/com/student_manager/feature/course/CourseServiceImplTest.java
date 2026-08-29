package com.student_manager.feature.course;

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
class CourseServiceImplTest {

    @Mock private CourseRepository repository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course existing;

    @BeforeEach
    void setUp() {
        existing = new Course();
        existing.setId(1L);
        existing.setCode("CS101");
        existing.setTitle("Intro to CS");
        existing.setCreditHours(5);
        existing.setStatus(CourseStatus.ACTIVE);
    }

    private CreateCourseRequest requestWith(String code) {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setCode(code);
        request.setTitle("Advanced CS");
        request.setCreditHours(4);
        request.setStatus(CourseStatus.ACTIVE);
        return request;
    }

    // ── create ──────────────────────────────────────────────────────

    @Test
    void createRejectsADuplicateCode() {
        CreateCourseRequest request = requestWith("CS101");
        when(repository.existsByCode("CS101")).thenReturn(true);

        assertThatThrownBy(() -> courseService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Course code already exists");

        verify(repository, never()).save(any());
    }

    // ── update ──────────────────────────────────────────────────────

    @Test
    void updateSucceedsWhenCodeDoesNotCollide() {
        CreateCourseRequest request = requestWith("CS102");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCodeAndIdNot("CS102", 1L)).thenReturn(false);
        when(repository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseDTO result = courseService.update(1L, request);

        assertThat(result.getCode()).isEqualTo("CS102");
    }

    @Test
    void updateRejectsACodeAlreadyUsedByAnotherCourse() {
        CreateCourseRequest request = requestWith("CS999");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCodeAndIdNot("CS999", 1L)).thenReturn(true);

        assertThatThrownBy(() -> courseService.update(1L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Course code already exists: CS999");

        verify(repository, never()).save(any());
    }

    @Test
    void updateThrowsWhenCourseDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.update(404L, requestWith("CS1")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ──────────────────────────────────────────────────────

    @Test
    void deleteThrowsWhenCourseDoesNotExist() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> courseService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}

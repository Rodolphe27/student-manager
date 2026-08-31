package com.student_manager.feature.enrollment;

import com.student_manager.feature.course.Course;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.course.CourseStatus;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
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
class EnrollmentServiceImplTest {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private Student student;
    private Course activeCourse;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setFirstName("Ada");
        student.setLastName("Lovelace");
        student.setEmail("ada@example.com");
        student.setMatriculationNumber("M-1");

        activeCourse = new Course();
        activeCourse.setId(10L);
        activeCourse.setCode("CS101");
        activeCourse.setTitle("Intro to CS");
        activeCourse.setCreditHours(5);
        activeCourse.setStatus(CourseStatus.ACTIVE);
    }

    private Enrollment enrollmentWith(EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(100L);
        enrollment.setStudent(student);
        enrollment.setCourse(activeCourse);
        enrollment.setStatus(status);
        enrollment.setGrade(Grade.NOT_GRADED);
        return enrollment;
    }

    // ── create ──────────────────────────────────────────────────────

    @Test
    void createEnrollsAStudentInAnActiveCourseAsPending() {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest();
        request.setStudentId(1L);
        request.setCourseId(10L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(activeCourse));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
            Enrollment e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        EnrollmentDTO result = enrollmentService.create(request);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.getGrade()).isEqualTo(Grade.NOT_GRADED);
        assertThat(result.getStudentId()).isEqualTo(1L);
        assertThat(result.getCourseId()).isEqualTo(10L);
    }

    @Test
    void createRejectsEnrollmentInAnInactiveCourse() {
        activeCourse.setStatus(CourseStatus.ARCHIVED);
        CreateEnrollmentRequest request = new CreateEnrollmentRequest();
        request.setStudentId(1L);
        request.setCourseId(10L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(activeCourse));

        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not active");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createRejectsADuplicateEnrollment() {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest();
        request.setStudentId(1L);
        request.setCourseId(10L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(activeCourse));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already enrolled");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenStudentDoesNotExist() {
        CreateEnrollmentRequest request = new CreateEnrollmentRequest();
        request.setStudentId(404L);
        request.setCourseId(10L);

        when(studentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── confirm ─────────────────────────────────────────────────────

    @Test
    void confirmMovesAPendingEnrollmentToConfirmed() {
        Enrollment pending = enrollmentWith(EnrollmentStatus.PENDING);
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentDTO result = enrollmentService.confirm(100L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    }

    @Test
    void confirmRejectsACancelledEnrollment() {
        Enrollment cancelled = enrollmentWith(EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> enrollmentService.confirm(100L))
                .isInstanceOf(ValidationException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void confirmRejectsAnAlreadyConfirmedEnrollment() {
        Enrollment confirmed = enrollmentWith(EnrollmentStatus.CONFIRMED);
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> enrollmentService.confirm(100L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already confirmed");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void confirmThrowsWhenEnrollmentDoesNotExist() {
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.confirm(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cancel ──────────────────────────────────────────────────────

    @Test
    void cancelRejectsAnAlreadyCancelledEnrollment() {
        Enrollment cancelled = enrollmentWith(EnrollmentStatus.CANCELLED);
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> enrollmentService.cancel(100L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelMovesAConfirmedEnrollmentToCancelled() {
        Enrollment confirmed = enrollmentWith(EnrollmentStatus.CONFIRMED);
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(confirmed));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentDTO result = enrollmentService.cancel(100L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void cancelClearsAnyLetterGrade() {
        Enrollment graded = enrollmentWith(EnrollmentStatus.CONFIRMED);
        graded.setGrade(Grade.A);
        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(graded));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentDTO result = enrollmentService.cancel(100L);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(result.getGrade()).isEqualTo(Grade.NOT_GRADED);
    }

    // ── updateGrade ─────────────────────────────────────────────────

    @Test
    void updateGradeSucceedsOnlyForConfirmedEnrollments() {
        Enrollment confirmed = enrollmentWith(EnrollmentStatus.CONFIRMED);
        UpdateGradeRequest request = new UpdateGradeRequest();
        request.setGrade(Grade.A);

        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(confirmed));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentDTO result = enrollmentService.updateGrade(100L, request);

        assertThat(result.getGrade()).isEqualTo(Grade.A);
    }

    @Test
    void updateGradeRejectsAPendingEnrollment() {
        Enrollment pending = enrollmentWith(EnrollmentStatus.PENDING);
        UpdateGradeRequest request = new UpdateGradeRequest();
        request.setGrade(Grade.A);

        when(enrollmentRepository.findById(100L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> enrollmentService.updateGrade(100L, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("confirmed");

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void updateGradeThrowsWhenEnrollmentDoesNotExist() {
        UpdateGradeRequest request = new UpdateGradeRequest();
        request.setGrade(Grade.A);
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.updateGrade(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ──────────────────────────────────────────────────────

    @Test
    void deleteThrowsWhenEnrollmentDoesNotExist() {
        when(enrollmentRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> enrollmentService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(enrollmentRepository, never()).deleteById(any());
    }
}

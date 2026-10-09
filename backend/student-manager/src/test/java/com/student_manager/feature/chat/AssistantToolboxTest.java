package com.student_manager.feature.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student_manager.feature.course.CourseDTO;
import com.student_manager.feature.course.CourseService;
import com.student_manager.feature.enrollment.EnrollmentDTO;
import com.student_manager.feature.enrollment.EnrollmentService;
import com.student_manager.feature.student.StudentDTO;
import com.student_manager.feature.student.StudentService;
import com.student_manager.shared.security.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssistantToolboxTest {

    private final CourseService courses = mock(CourseService.class);
    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final StudentService students = mock(StudentService.class);
    private final OwnershipGuard guard = mock(OwnershipGuard.class);
    private final PendingActionStore store = new PendingActionStore();

    private static Authentication as(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private AssistantToolbox toolbox(Authentication auth) {
        return new AssistantToolbox(courses, enrollments, students, guard, store, new ObjectMapper().findAndRegisterModules(), auth);
    }

    @BeforeEach
    void aStudentRecord() {
        StudentDTO student = new StudentDTO();
        student.setId(5L);
        when(students.findByAccountUsername("alice")).thenReturn(student);
        CourseDTO course = new CourseDTO();
        course.setId(9L);
        course.setCode("CS101");
        course.setTitle("Intro");
        when(courses.findById(9L)).thenReturn(course);
    }

    @Test
    void aStudentIsOfferedTheEnrollTool_staffIsNot() {
        assertThat(toolbox(as("alice", "STUDENT")).specs()).extracting(AssistantClient.ToolSpec::name).contains("propose_enrollment");
        assertThat(toolbox(as("tom", "TEACHER")).specs()).extracting(AssistantClient.ToolSpec::name).doesNotContain("propose_enrollment");
    }

    @Test
    void enrollingRecordsAProposalAndChangesNothing() {
        when(guard.canEnroll(anyLong(), anyLong(), any())).thenReturn(true);
        AssistantToolbox box = toolbox(as("alice", "STUDENT"));

        String result = box.run("propose_enrollment", Map.of("courseId", 9));

        assertThat(result).contains("confirm");
        assertThat(box.proposed()).hasSize(1);
        assertThat(box.proposed().get(0).description()).contains("CS101");
        verify(enrollments, never()).create(any());
    }

    @Test
    void enrollingWithoutTheRightIsRefusedAndRecordsNothing() {
        when(guard.canEnroll(anyLong(), anyLong(), any())).thenReturn(false);
        AssistantToolbox box = toolbox(as("alice", "STUDENT"));

        assertThat(box.run("propose_enrollment", Map.of("courseId", 9))).startsWith("Error");
        assertThat(box.proposed()).isEmpty();
    }

    @Test
    void staffCannotEnrollThroughTheTool() {
        AssistantToolbox box = toolbox(as("tom", "TEACHER"));

        assertThat(box.run("propose_enrollment", Map.of("courseId", 9))).startsWith("Error");
        assertThat(box.proposed()).isEmpty();
    }

    @Test
    void cancelChecksTheRightBeforeLookingTheEnrollmentUp() {
        when(guard.canCancelEnrollment(anyLong(), any())).thenReturn(false);
        AssistantToolbox box = toolbox(as("alice", "STUDENT"));

        assertThat(box.run("propose_cancel_enrollment", Map.of("enrollmentId", 3))).startsWith("Error");
        verify(enrollments, never()).findById(anyLong());
        assertThat(box.proposed()).isEmpty();
    }

    @Test
    void cancelRecordsAProposal() {
        when(guard.canCancelEnrollment(anyLong(), any())).thenReturn(true);
        EnrollmentDTO e = new EnrollmentDTO();
        e.setCourseCode("CS101");
        e.setCourseTitle("Intro");
        when(enrollments.findById(3L)).thenReturn(e);
        AssistantToolbox box = toolbox(as("alice", "STUDENT"));

        box.run("propose_cancel_enrollment", Map.of("enrollmentId", 3));

        assertThat(box.proposed()).hasSize(1);
        verify(enrollments, never()).cancel(anyLong());
    }

    @Test
    void aStudentSeesOnlyTheirOwnEnrollments() {
        when(enrollments.findByStudentId(5L)).thenReturn(List.of());
        toolbox(as("alice", "STUDENT")).run("get_my_enrollments", Map.of());

        verify(enrollments).findByStudentId(5L);
    }

    @Test
    void aBadIdIsAnErrorNotACrash() {
        AssistantToolbox box = toolbox(as("alice", "STUDENT"));

        assertThat(box.run("propose_enrollment", Map.of("courseId", "nine"))).startsWith("Error");
        assertThat(box.run("nope", Map.of())).startsWith("Error");
    }
}

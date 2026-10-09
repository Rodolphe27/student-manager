package com.student_manager.feature.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student_manager.feature.course.CourseService;
import com.student_manager.feature.enrollment.CreateEnrollmentRequest;
import com.student_manager.feature.enrollment.EnrollmentDTO;
import com.student_manager.feature.enrollment.EnrollmentService;
import com.student_manager.feature.enrollment.EnrollmentStatus;
import com.student_manager.feature.student.StudentDTO;
import com.student_manager.feature.student.StudentService;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.security.OwnershipGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final AssistantClient model = mock(AssistantClient.class);
    private final EnrollmentService enrollments = mock(EnrollmentService.class);
    private final StudentService students = mock(StudentService.class);
    private final OwnershipGuard guard = mock(OwnershipGuard.class);
    private final PendingActionStore store = new PendingActionStore();
    private final Authentication alice = new UsernamePasswordAuthenticationToken("alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    private final Authentication mallory = new UsernamePasswordAuthenticationToken("mallory", "n/a", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    private ChatService service;

    @BeforeEach
    void setUp() {
        when(model.isConfigured()).thenReturn(true);
        StudentDTO student = new StudentDTO();
        student.setId(5L);
        when(students.findByAccountUsername("alice")).thenReturn(student);
        service = new ChatService(model, mock(CourseService.class), enrollments, students, guard, store,
                new ObjectMapper(), Clock.fixed(Instant.parse("2026-10-09T08:00:00Z"), ZoneOffset.UTC));
    }

    private static ChatRequest says(String text) {
        return new ChatRequest(List.of(new ChatMessage("user", text)));
    }

    @Test
    void withoutAnApiKeyTheAssistantIsUnavailable() {
        when(model.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.chat(says("hi"), alice)).isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void theFirstMessageMustComeFromTheUser() {
        ChatRequest request = new ChatRequest(List.of(new ChatMessage("assistant", "hello"), new ChatMessage("user", "hi")));

        assertThatThrownBy(() -> service.chat(request, alice)).isInstanceOf(ValidationException.class);
    }

    @Test
    void theLastMessageMustComeFromTheUser() {
        ChatRequest request = new ChatRequest(List.of(new ChatMessage("user", "hi"), new ChatMessage("assistant", "hello")));

        assertThatThrownBy(() -> service.chat(request, alice)).isInstanceOf(ValidationException.class);
    }

    @Test
    void theModelAnswerIsReturned() {
        when(model.reply(any(), any(), any())).thenReturn("Hello Alice");

        assertThat(service.chat(says("hi"), alice).reply()).isEqualTo("Hello Alice");
    }

    @Test
    void theSystemPromptNamesTheCallersRole() {
        when(model.reply(any(), any(), any())).thenReturn("ok");
        service.chat(says("hi"), alice);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(model).reply(prompt.capture(), any(), any());
        assertThat(prompt.getValue()).contains("STUDENT").contains("alice");
    }

    @Test
    void anEleventhMessageInAMinuteIsRefused() {
        when(model.reply(any(), any(), any())).thenReturn("ok");
        for (int i = 0; i < ChatService.REQUESTS_PER_MINUTE; i++) {
            service.chat(says("hi"), alice);
        }

        assertThatThrownBy(() -> service.chat(says("hi"), alice)).isInstanceOf(TooManyChatRequestsException.class);
        // The limit is per user.
        assertThat(service.chat(says("hi"), mallory).reply()).isEqualTo("ok");
    }

    @Test
    void confirmingAnEnrollmentChecksTheRightAgainAndEnrolls() {
        PendingAction action = store.propose("alice", "ENROLL", Map.of("courseId", 9L), "Enroll");
        when(guard.canEnroll(anyLong(), anyLong(), any())).thenReturn(true);
        EnrollmentDTO done = new EnrollmentDTO();
        done.setCourseCode("CS101");
        done.setCourseTitle("Intro");
        done.setStatus(EnrollmentStatus.PENDING);
        when(enrollments.create(any())).thenReturn(done);

        ConfirmResponse response = service.confirm(action.id(), alice);

        ArgumentCaptor<CreateEnrollmentRequest> sent = ArgumentCaptor.forClass(CreateEnrollmentRequest.class);
        verify(enrollments).create(sent.capture());
        assertThat(sent.getValue().getStudentId()).isEqualTo(5L);
        assertThat(sent.getValue().getCourseId()).isEqualTo(9L);
        assertThat(response.message()).contains("CS101");
    }

    @Test
    void ifTheRightWasLostByConfirmTimeNothingHappens() {
        PendingAction action = store.propose("alice", "ENROLL", Map.of("courseId", 9L), "Enroll");
        when(guard.canEnroll(anyLong(), anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.confirm(action.id(), alice)).isInstanceOf(AccessDeniedException.class);
        verify(enrollments, never()).create(any());
    }

    @Test
    void aProposalCannotBeConfirmedTwiceOrByAnotherUser() {
        PendingAction action = store.propose("alice", "CANCEL", Map.of("enrollmentId", 3L), "Cancel");
        when(guard.canCancelEnrollment(anyLong(), any())).thenReturn(true);
        EnrollmentDTO done = new EnrollmentDTO();
        when(enrollments.cancel(3L)).thenReturn(done);

        assertThatThrownBy(() -> service.confirm(action.id(), mallory)).isInstanceOf(ResourceNotFoundException.class);
        service.confirm(action.id(), alice);
        assertThatThrownBy(() -> service.confirm(action.id(), alice)).isInstanceOf(ResourceNotFoundException.class);
        verify(enrollments).cancel(3L);
    }
}

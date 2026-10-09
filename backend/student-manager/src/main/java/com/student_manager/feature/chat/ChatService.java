package com.student_manager.feature.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.course.CourseService;
import com.student_manager.feature.enrollment.CreateEnrollmentRequest;
import com.student_manager.feature.enrollment.EnrollmentDTO;
import com.student_manager.feature.enrollment.EnrollmentService;
import com.student_manager.feature.student.StudentService;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.security.OwnershipGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs one chat turn and the confirmation of changes the assistant proposed.
 *
 * <p>The model never changes data. It can read what the signed-in user may read, and propose a
 * change; the change runs in {@link #confirm} only when the user presses the button, after the
 * same ownership checks the REST API applies are made again.
 */
@Slf4j
@Service
public class ChatService {

    static final int MAX_MESSAGES = 20;
    static final int MAX_MESSAGE_CHARS = 2000;
    static final int REQUESTS_PER_MINUTE = 10;

    private final AssistantClient client;
    private final CourseService courses;
    private final EnrollmentService enrollments;
    private final StudentService students;
    private final OwnershipGuard guard;
    private final PendingActionStore store;
    private final ObjectMapper json;
    private final Clock clock;
    private final Map<String, Deque<Instant>> recent = new ConcurrentHashMap<>();

    @Autowired
    public ChatService(AssistantClient client, CourseService courses, EnrollmentService enrollments,
                       StudentService students, OwnershipGuard guard, PendingActionStore store, ObjectMapper json) {
        this(client, courses, enrollments, students, guard, store, json, Clock.systemUTC());
    }

    ChatService(AssistantClient client, CourseService courses, EnrollmentService enrollments,
                StudentService students, OwnershipGuard guard, PendingActionStore store,
                ObjectMapper json, Clock clock) {
        this.client = client;
        this.courses = courses;
        this.enrollments = enrollments;
        this.students = students;
        this.guard = guard;
        this.store = store;
        this.json = json;
        this.clock = clock;
    }

    public boolean isAvailable() {
        return client.isConfigured();
    }

    public ChatResponse chat(ChatRequest request, Authentication auth) {
        if (!client.isConfigured()) {
            throw new AssistantUnavailableException("The assistant is not set up on this server.");
        }
        List<ChatMessage> history = request.messages();
        if (!"user".equals(history.get(0).role()) || !"user".equals(history.get(history.size() - 1).role())) {
            throw new ValidationException("The conversation must start and end with a message from the user");
        }
        enforceRateLimit(auth.getName());

        AssistantToolbox tools = new AssistantToolbox(courses, enrollments, students, guard, store, json, auth);
        String reply = client.reply(AssistantPrompt.forUser(auth.getName(), roleOf(auth)), history, tools);
        return new ChatResponse(reply, tools.proposed());
    }

    /** Runs a proposal the user confirmed. Authorization is checked again here, not remembered from the proposal. */
    public ConfirmResponse confirm(String actionId, Authentication auth) {
        PendingActionStore.Entry entry = store.take(actionId, auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("This proposal has expired or was already used"));
        return switch (entry.type()) {
            case "ENROLL" -> enroll(entry.params().get("courseId"), auth);
            case "CANCEL" -> cancel(entry.params().get("enrollmentId"), auth);
            default -> throw new ValidationException("Unknown action");
        };
    }

    private ConfirmResponse enroll(Long courseId, Authentication auth) {
        long studentId = students.findByAccountUsername(auth.getName()).getId();
        if (!guard.canEnroll(studentId, courseId, auth)) {
            throw new AccessDeniedException("Not allowed to enroll in this course");
        }
        CreateEnrollmentRequest request = new CreateEnrollmentRequest();
        request.setStudentId(studentId);
        request.setCourseId(courseId);
        EnrollmentDTO done = enrollments.create(request);
        return new ConfirmResponse("Enrolled in " + done.getCourseCode() + " " + done.getCourseTitle()
                + ". Status: " + done.getStatus() + ".");
    }

    private ConfirmResponse cancel(Long enrollmentId, Authentication auth) {
        if (!guard.canCancelEnrollment(enrollmentId, auth)) {
            throw new AccessDeniedException("Not allowed to cancel this enrollment");
        }
        EnrollmentDTO done = enrollments.cancel(enrollmentId);
        return new ConfirmResponse("Cancelled the enrollment in " + done.getCourseCode() + " " + done.getCourseTitle() + ".");
    }

    private void enforceRateLimit(String username) {
        Instant now = clock.instant();
        Deque<Instant> times = recent.computeIfAbsent(username, u -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(Duration.ofMinutes(1)))) {
                times.removeFirst();
            }
            if (times.size() >= REQUESTS_PER_MINUTE) {
                throw new TooManyChatRequestsException("Too many messages. Please wait a moment and try again.");
            }
            times.addLast(now);
        }
    }

    private static String roleOf(Authentication auth) {
        for (Role role : Role.values()) {
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()))) {
                return role.name();
            }
        }
        return "UNKNOWN";
    }
}

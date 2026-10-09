package com.student_manager.feature.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.course.CourseDTO;
import com.student_manager.feature.course.CourseService;
import com.student_manager.feature.course.CourseStatus;
import com.student_manager.feature.enrollment.EnrollmentDTO;
import com.student_manager.feature.enrollment.EnrollmentService;
import com.student_manager.feature.student.StudentService;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.security.OwnershipGuard;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The tools the model may use during one chat turn, bound to the signed-in user.
 *
 * <p>Reads go through the same services as the REST API and are limited by the same rules
 * (a student sees only their own enrollments, a teacher only their own courses). Writes are
 * never executed here: a tool that would change data only records a {@link PendingAction},
 * which runs when the user confirms it in the UI. Whatever the model asks for, the caller's
 * rights are checked here, not trusted from the prompt.
 */
class AssistantToolbox implements AssistantClient.Toolbox {

    private static final int COURSE_LIMIT = 10;
    private static final int ENROLLMENT_LIMIT = 20;

    private final CourseService courses;
    private final EnrollmentService enrollments;
    private final StudentService students;
    private final OwnershipGuard guard;
    private final PendingActionStore store;
    private final ObjectMapper json;
    private final Authentication auth;
    private final List<PendingAction> proposed = new ArrayList<>();

    AssistantToolbox(CourseService courses, EnrollmentService enrollments, StudentService students,
                     OwnershipGuard guard, PendingActionStore store, ObjectMapper json, Authentication auth) {
        this.courses = courses;
        this.enrollments = enrollments;
        this.students = students;
        this.guard = guard;
        this.store = store;
        this.json = json;
        this.auth = auth;
    }

    /** The changes proposed so far in this turn. */
    List<PendingAction> proposed() {
        return List.copyOf(proposed);
    }

    private boolean isStudent() {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.STUDENT.name()));
    }

    @Override
    public List<AssistantClient.ToolSpec> specs() {
        List<AssistantClient.ToolSpec> specs = new ArrayList<>();
        specs.add(new AssistantClient.ToolSpec("search_courses",
                "Search the course catalogue by title or code. Returns at most " + COURSE_LIMIT + " courses.",
                Map.of("query", Map.of("type", "string", "description", "Text to look for in the title or code. Leave out to list courses."),
                        "status", Map.of("type", "string", "enum", List.of("ACTIVE", "INACTIVE", "ARCHIVED"),
                                "description", "Only courses with this status. Usually ACTIVE.")),
                List.of()));
        specs.add(new AssistantClient.ToolSpec("get_my_enrollments",
                isStudent()
                        ? "List the signed-in student's own enrollments with status and grade."
                        : "List recent enrollments in the courses the signed-in user may manage.",
                Map.of(), List.of()));
        if (isStudent()) {
            specs.add(new AssistantClient.ToolSpec("propose_enrollment",
                    "Propose enrolling the signed-in student in a course. Nothing happens until the student confirms.",
                    Map.of("courseId", Map.of("type", "integer", "description", "Id of the course, from search_courses.")),
                    List.of("courseId")));
        }
        specs.add(new AssistantClient.ToolSpec("propose_cancel_enrollment",
                "Propose cancelling an enrollment. Nothing happens until the user confirms.",
                Map.of("enrollmentId", Map.of("type", "integer", "description", "Id of the enrollment, from get_my_enrollments.")),
                List.of("enrollmentId")));
        return specs;
    }

    @Override
    public String run(String name, Map<String, Object> input) {
        try {
            return switch (name) {
                case "search_courses" -> searchCourses(input);
                case "get_my_enrollments" -> myEnrollments();
                case "propose_enrollment" -> proposeEnrollment(id(input, "courseId"));
                case "propose_cancel_enrollment" -> proposeCancel(id(input, "enrollmentId"));
                default -> "Error: unknown tool " + name;
            };
        } catch (ResourceNotFoundException | ValidationException e) {
            return "Error: " + e.getMessage();
        } catch (JsonProcessingException e) {
            return "Error: could not format the result";
        }
    }

    private static long id(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            return n.longValue();
        }
        throw new ValidationException(key + " must be a whole number");
    }

    private String searchCourses(Map<String, Object> input) throws JsonProcessingException {
        String query = input.get("query") instanceof String s && !s.isBlank() ? s.strip() : null;
        CourseStatus status = null;
        if (input.get("status") instanceof String s && !s.isBlank()) {
            try {
                status = CourseStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("status must be ACTIVE, INACTIVE or ARCHIVED");
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CourseDTO c : courses.search(query, status, PageRequest.of(0, COURSE_LIMIT, Sort.by("title"))).getContent()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("code", c.getCode());
            row.put("title", c.getTitle());
            row.put("creditHours", c.getCreditHours());
            row.put("status", c.getStatus());
            row.put("teacher", c.getTeacherName());
            row.put("term", c.getTermName());
            rows.add(row);
        }
        return json.writeValueAsString(rows);
    }

    private String myEnrollments() throws JsonProcessingException {
        List<EnrollmentDTO> found;
        if (isStudent()) {
            found = enrollments.findByStudentId(students.findByAccountUsername(auth.getName()).getId());
        } else {
            found = enrollments.search(null, null, null, guard.teacherScope(auth),
                    PageRequest.of(0, ENROLLMENT_LIMIT, Sort.by(Sort.Direction.DESC, "enrolledAt", "id"))).getContent();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EnrollmentDTO e : found.stream().limit(ENROLLMENT_LIMIT).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getId());
            if (!isStudent()) {
                row.put("student", e.getStudentName());
            }
            row.put("course", e.getCourseCode() + " " + e.getCourseTitle());
            row.put("courseId", e.getCourseId());
            row.put("status", e.getStatus());
            row.put("grade", e.getGrade());
            row.put("enrolledAt", e.getEnrolledAt());
            rows.add(row);
        }
        return json.writeValueAsString(rows);
    }

    private String proposeEnrollment(long courseId) {
        if (!isStudent()) {
            return "Error: only students can enroll themselves through the assistant.";
        }
        long studentId = students.findByAccountUsername(auth.getName()).getId();
        CourseDTO course = courses.findById(courseId);
        if (!guard.canEnroll(studentId, courseId, auth)) {
            return "Error: you are not allowed to enroll in this course.";
        }
        PendingAction action = store.propose(auth.getName(), "ENROLL", Map.of("courseId", courseId),
                "Enroll in " + course.getCode() + " " + course.getTitle());
        proposed.add(action);
        return "Proposal prepared: \"" + action.description() + "\". The student must press the confirm button; tell them so. It is not done yet.";
    }

    private String proposeCancel(long enrollmentId) {
        // Check the right first: for someone else's enrollment nothing about it may be revealed.
        if (!guard.canCancelEnrollment(enrollmentId, auth)) {
            return "Error: you are not allowed to cancel this enrollment (students can only cancel their own pending enrollments).";
        }
        EnrollmentDTO e = enrollments.findById(enrollmentId);
        PendingAction action = store.propose(auth.getName(), "CANCEL", Map.of("enrollmentId", enrollmentId),
                "Cancel the enrollment in " + e.getCourseCode() + " " + e.getCourseTitle()
                        + (isStudent() ? "" : " for " + e.getStudentName()));
        proposed.add(action);
        return "Proposal prepared: \"" + action.description() + "\". The user must press the confirm button; tell them so. It is not done yet.";
    }
}

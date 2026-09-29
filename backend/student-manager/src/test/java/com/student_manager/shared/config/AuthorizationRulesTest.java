package com.student_manager.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.auth.UserRepository;
import com.student_manager.feature.course.Course;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.course.CourseStatus;
import com.student_manager.feature.course.CreateCourseRequest;
import com.student_manager.feature.enrollment.CreateEnrollmentRequest;
import com.student_manager.feature.enrollment.Enrollment;
import com.student_manager.feature.enrollment.EnrollmentRepository;
import com.student_manager.feature.enrollment.EnrollmentStatus;
import com.student_manager.feature.enrollment.Grade;
import com.student_manager.feature.enrollment.UpdateGradeRequest;
import com.student_manager.feature.student.CreateStudentRequest;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.feature.teacher.Teacher;
import com.student_manager.feature.teacher.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the role matrix enforced by {@link SecurityConfig} (issue #31).
 * A STUDENT-role session must be rejected with 403 on staff/admin-only endpoints,
 * while TEACHER / ADMIN sessions are allowed through the authorization layer.
 * Also covers the session lifecycle (login → /me → logout), 401 without a
 * session, and CSRF enforcement on state-changing requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationRulesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;

    /** An authenticated request (as "{role}-user") carrying a valid CSRF token. */
    private RequestPostProcessor loggedInAs(String role) {
        return loggedInAs(role.toLowerCase() + "-user", role);
    }

    /**
     * An authenticated request carrying a valid CSRF token. The CSRF token is always
     * attached so a wrong-role request is rejected by the role rules (403), not by a
     * missing CSRF token — otherwise every "is forbidden" test would pass vacuously.
     */
    private RequestPostProcessor loggedInAs(String username, String role) {
        RequestPostProcessor principal = user(username).roles(role);
        return request -> csrf().postProcessRequest(principal.postProcessRequest(request));
    }

    // --- Authentication (401) vs. authorization (403) ---
    // No / invalid session must be 401 so the frontend's 401 interceptor logs the
    // user out; 403 stays reserved for a logged-in user with the wrong role.

    @Test
    void noSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownSessionCookieReturns401() throws Exception {
        mockMvc.perform(get("/api/students").cookie(new Cookie("SESSION", "bm8tc3VjaC1zZXNzaW9u")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(user("admin-user").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginStartsASessionThatMeAndLogoutUse() throws Exception {
        User account = new User();
        account.setUsername("session-user");
        account.setEmail("session-user@example.com");
        account.setPasswordHash(passwordEncoder.encode("Password123!"));
        account.setRole(Role.ADMIN);
        account.setActive(true);
        userRepository.save(account);

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"session-user\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("session-user"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn();
        Cookie session = login.getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();
        assertThat(session.isHttpOnly()).isTrue();

        // The session cookie alone authenticates follow-up requests, with the stored role.
        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mockMvc.perform(get("/api/students").cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").cookie(session).with(csrf()))
                .andExpect(status().isNoContent());

        // The server-side session is gone — the old cookie no longer works.
        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    /** Persists a User + Student sharing an e-mail and returns the student id. */
    private long linkedStudentFor(String username, String email) {
        User account = new User();
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash("irrelevant-for-authz");
        account.setRole(Role.STUDENT);
        account.setActive(true);
        userRepository.save(account);

        Student student = new Student();
        student.setFirstName("Owned");
        student.setLastName("Student");
        student.setMatriculationNumber("M-OWN-" + username);
        student.setEmail(email);
        return studentRepository.save(student).getId();
    }

    /** Persists an ACTIVE course with a unique code and returns its id. */
    private long activeCourseFor(String codeSuffix) {
        Course course = new Course();
        course.setCode("AUTHZ-ENR-" + codeSuffix);
        course.setTitle("Authorization Enrollment Fixture");
        course.setCreditHours(3);
        course.setStatus(CourseStatus.ACTIVE);
        return courseRepository.save(course).getId();
    }

    /** Persists a User + Teacher linked via the account FK (not email matching, unlike students). */
    private Teacher linkedTeacherFor(String username, String email) {
        User account = new User();
        account.setUsername(username);
        account.setEmail(email);
        account.setPasswordHash("irrelevant-for-authz");
        account.setRole(Role.TEACHER);
        account.setActive(true);
        User savedAccount = userRepository.save(account);

        Teacher teacher = new Teacher();
        teacher.setFirstName("Owning");
        teacher.setLastName("Teacher");
        teacher.setEmail(email);
        teacher.setAccount(savedAccount);
        return teacherRepository.save(teacher);
    }

    /** Persists an ACTIVE course taught by the given teacher and returns its id. */
    private long courseTaughtBy(Teacher teacher, String codeSuffix) {
        Course course = new Course();
        course.setCode("AUTHZ-OWN-" + codeSuffix);
        course.setTitle("Ownership Fixture");
        course.setCreditHours(3);
        course.setStatus(CourseStatus.ACTIVE);
        course.setTeacher(teacher);
        return courseRepository.save(course).getId();
    }

    /** Persists a PENDING enrollment linking the given student and course, and returns its id. */
    private long enrollmentFor(long studentId, long courseId) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(studentRepository.findById(studentId).orElseThrow());
        enrollment.setCourse(courseRepository.findById(courseId).orElseThrow());
        enrollment.setEnrolledAt(LocalDate.now());
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollment.setGrade(Grade.NOT_GRADED);
        return enrollmentRepository.save(enrollment).getId();
    }

    /** Persists a bare Student row with no linked account, for invite-claiming tests. */
    private long unclaimedStudentFor(String suffix) {
        Student student = new Student();
        student.setFirstName("Invite");
        student.setLastName("Target");
        student.setMatriculationNumber("M-INV-" + suffix);
        student.setEmail("invite." + suffix + "@example.com");
        return studentRepository.save(student).getId();
    }

    // ── students ────────────────────────────────────────────────────

    @Test
    void studentCannotListStudents() throws Exception {
        mockMvc.perform(get("/api/students").with(loggedInAs("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateStudents() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(loggedInAs("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanListStudents() throws Exception {
        mockMvc.perform(get("/api/students").with(loggedInAs("TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void studentMayReachTheirOwnRecordEndpoint() throws Exception {
        // Authorization lets a STUDENT through to /api/students/me; the request
        // then 404s only because this token has no matching account/student row.
        mockMvc.perform(get("/api/students/me").with(loggedInAs("STUDENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void teacherCannotCreateStudents() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(loggedInAs("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateStudents() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(loggedInAs("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isCreated());
    }

    // ── courses ─────────────────────────────────────────────────────

    @Test
    void studentCanBrowseCourses() throws Exception {
        mockMvc.perform(get("/api/courses").with(loggedInAs("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotCreateCourses() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(loggedInAs("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCourse())))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanCreateCourses() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(loggedInAs("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCourse())))
                .andExpect(status().isCreated());
    }

    // ── enrollments ─────────────────────────────────────────────────

    @Test
    void studentCannotListAllEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments").with(loggedInAs("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanLookUpTheirOwnEnrollments() throws Exception {
        long ownId = linkedStudentFor("owner-user", "owner.authz@example.com");

        mockMvc.perform(get("/api/enrollments/student/" + ownId)
                        .with(loggedInAs("owner-user", "STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotLookUpAnotherStudentsEnrollments() throws Exception {
        long ownId = linkedStudentFor("owner2-user", "owner2.authz@example.com");
        long someoneElse = ownId + 999;

        mockMvc.perform(get("/api/enrollments/student/" + someoneElse)
                        .with(loggedInAs("owner2-user", "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentWithNoLinkedRecordCannotListEnrollmentsByStudentId() throws Exception {
        // "student-user" has a valid token but no account/student row behind it.
        mockMvc.perform(get("/api/enrollments/student/1").with(loggedInAs("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotDeleteEnrollments() throws Exception {
        mockMvc.perform(delete("/api/enrollments/1").with(loggedInAs("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteEnrollments() throws Exception {
        mockMvc.perform(delete("/api/enrollments/1").with(loggedInAs("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanListAllEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments").with(loggedInAs("TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCanEnrollThemselves() throws Exception {
        long ownId = linkedStudentFor("self-enroll-user", "self-enroll.authz@example.com");
        long courseId = activeCourseFor("1");

        CreateEnrollmentRequest request = new CreateEnrollmentRequest();
        request.setStudentId(ownId);
        request.setCourseId(courseId);

        mockMvc.perform(post("/api/enrollments")
                        .with(loggedInAs("self-enroll-user", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void studentCannotEnrollAnotherStudent() throws Exception {
        long ownId = linkedStudentFor("self-enroll-user2", "self-enroll2.authz@example.com");
        long someoneElse = ownId + 999;
        long courseId = activeCourseFor("2");

        CreateEnrollmentRequest request = new CreateEnrollmentRequest();
        request.setStudentId(someoneElse);
        request.setCourseId(courseId);

        mockMvc.perform(post("/api/enrollments")
                        .with(loggedInAs("self-enroll-user2", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── ownership: course rosters (OwnershipGuard.canAccessCourseData) ─

    @Test
    void teacherCanViewTheRosterOfACourseTheyTeach() throws Exception {
        Teacher teacher = linkedTeacherFor("owning-teacher", "owning.teacher@example.com");
        long courseId = courseTaughtBy(teacher, "own1");

        mockMvc.perform(get("/api/enrollments/course/" + courseId)
                        .with(loggedInAs("owning-teacher", "TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotViewTheRosterOfAnotherTeachersCourse() throws Exception {
        Teacher otherTeacher = linkedTeacherFor("other-teacher", "other.teacher@example.com");
        long courseId = courseTaughtBy(otherTeacher, "own2");

        mockMvc.perform(get("/api/enrollments/course/" + courseId)
                        .with(loggedInAs("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherRequestingANonexistentCourseRosterGetsAnEmptyListNotForbidden() throws Exception {
        // Regression test for the OwnershipGuard fix: a TEACHER hitting a nonexistent
        // course id must see the same result ADMIN would (an empty roster), not a
        // misleading 403 that masks "doesn't exist" as "not yours".
        mockMvc.perform(get("/api/enrollments/course/999999")
                        .with(loggedInAs("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void adminCanViewTheRosterOfAnyCourse() throws Exception {
        Teacher teacher = linkedTeacherFor("admin-view-teacher", "admin.view.teacher@example.com");
        long courseId = courseTaughtBy(teacher, "own3");

        mockMvc.perform(get("/api/enrollments/course/" + courseId)
                        .with(loggedInAs("ADMIN")))
                .andExpect(status().isOk());
    }

    // ── ownership: enrollment management (OwnershipGuard.canManageEnrollment) ─

    @Test
    void teacherCanConfirmAnEnrollmentInACourseTheyTeach() throws Exception {
        Teacher teacher = linkedTeacherFor("confirm-teacher", "confirm.teacher@example.com");
        long courseId = courseTaughtBy(teacher, "conf1");
        long studentId = linkedStudentFor("confirm-student", "confirm.student@example.com");
        long enrollmentId = enrollmentFor(studentId, courseId);

        mockMvc.perform(patch("/api/enrollments/" + enrollmentId + "/confirm")
                        .with(loggedInAs("confirm-teacher", "TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotConfirmAnEnrollmentInAnotherTeachersCourse() throws Exception {
        Teacher otherTeacher = linkedTeacherFor("other-confirm-teacher", "other.confirm.teacher@example.com");
        long courseId = courseTaughtBy(otherTeacher, "conf2");
        long studentId = linkedStudentFor("other-confirm-student", "other.confirm.student@example.com");
        long enrollmentId = enrollmentFor(studentId, courseId);

        mockMvc.perform(patch("/api/enrollments/" + enrollmentId + "/confirm")
                        .with(loggedInAs("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherGradingANonexistentEnrollmentGetsNotFoundNotForbidden() throws Exception {
        // Regression test for the OwnershipGuard fix: a TEACHER hitting a nonexistent
        // enrollment id must see the same 404 ADMIN would, not a misleading 403.
        UpdateGradeRequest request = new UpdateGradeRequest();
        request.setGrade(Grade.A);

        mockMvc.perform(patch("/api/enrollments/999999/grade")
                        .with(loggedInAs("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanConfirmAnEnrollmentInAnyCourse() throws Exception {
        Teacher teacher = linkedTeacherFor("admin-confirm-teacher", "admin.confirm.teacher@example.com");
        long courseId = courseTaughtBy(teacher, "conf3");
        long studentId = linkedStudentFor("admin-confirm-student", "admin.confirm.student@example.com");
        long enrollmentId = enrollmentFor(studentId, courseId);

        mockMvc.perform(patch("/api/enrollments/" + enrollmentId + "/confirm")
                        .with(loggedInAs("ADMIN")))
                .andExpect(status().isOk());
    }

    // ── registration invites ────────────────────────────────────────

    @Test
    void adminIssuedStudentInviteCanBeClaimedViaRegistration() throws Exception {
        long studentId = unclaimedStudentFor("claim1");

        String issueResponse = mockMvc.perform(post("/api/students/" + studentId + "/invite")
                        .with(loggedInAs("ADMIN")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String code = objectMapper.readTree(issueResponse).get("code").asText();

        String registerBody = """
                {"username":"claimed-student","email":"claimed.student@example.com","password":"Password123","registrationCode":"%s"}
                """.formatted(code);

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void teacherCannotIssueAStudentInvite() throws Exception {
        long studentId = unclaimedStudentFor("noaccess");

        mockMvc.perform(post("/api/students/" + studentId + "/invite")
                        .with(loggedInAs("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void claimingAMalformedRegistrationCodeIsRejectedAsABadRequest() throws Exception {
        // The registrationCode @Pattern rejects this before it ever reaches the invite
        // service — proves the new validation is actually wired into the real endpoint.
        String registerBody = """
                {"username":"bad-code-user","email":"bad.code.user@example.com","password":"Password123","registrationCode":"not-a-real-code"}
                """;

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void claimingAWellFormedButUnknownRegistrationCodeIsRejected() throws Exception {
        // Well-formed (passes the @Pattern) but doesn't exist — rejected by the invite
        // service itself, not the DTO validator.
        String unknownCode = "Z".repeat(32);
        String registerBody = """
                {"username":"unknown-code-user","email":"unknown.code.user@example.com","password":"Password123","registrationCode":"%s"}
                """.formatted(unknownCode);

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest());
    }

    // ── unauthenticated ─────────────────────────────────────────────

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().is4xxClientError());
    }

    // ── fixtures ────────────────────────────────────────────────────

    private CreateStudentRequest sampleStudent() {
        CreateStudentRequest r = new CreateStudentRequest();
        r.setFirstName("Grace");
        r.setLastName("Hopper");
        r.setMatriculationNumber("M-AUTHZ-1");
        r.setEmail("grace.authz@example.com");
        return r;
    }

    private CreateCourseRequest sampleCourse() {
        CreateCourseRequest r = new CreateCourseRequest();
        r.setCode("AUTHZ-101");
        r.setTitle("Authorization Basics");
        r.setCreditHours(3);
        return r;
    }
}

package com.student_manager.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student_manager.feature.auth.JwtUtil;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.auth.UserRepository;
import com.student_manager.feature.course.CreateCourseRequest;
import com.student_manager.feature.student.CreateStudentRequest;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the role matrix enforced by {@link SecurityConfig} (issue #31).
 * A STUDENT-role token must be rejected with 403 on staff/admin-only endpoints,
 * while TEACHER / ADMIN tokens are allowed through the authorization layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationRulesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;

    private String bearer(String role) {
        return "Bearer " + jwtUtil.generateToken(role.toLowerCase() + "-user", role);
    }

    private String bearerFor(String username, String role) {
        return "Bearer " + jwtUtil.generateToken(username, role);
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

    // ── students ────────────────────────────────────────────────────

    @Test
    void studentCannotListStudents() throws Exception {
        mockMvc.perform(get("/api/students").header("Authorization", bearer("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotCreateStudents() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", bearer("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanListStudents() throws Exception {
        mockMvc.perform(get("/api/students").header("Authorization", bearer("TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void studentMayReachTheirOwnRecordEndpoint() throws Exception {
        // Authorization lets a STUDENT through to /api/students/me; the request
        // then 404s only because this token has no matching account/student row.
        mockMvc.perform(get("/api/students/me").header("Authorization", bearer("STUDENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void teacherCannotCreateStudents() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", bearer("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateStudents() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleStudent())))
                .andExpect(status().isCreated());
    }

    // ── courses ─────────────────────────────────────────────────────

    @Test
    void studentCanBrowseCourses() throws Exception {
        mockMvc.perform(get("/api/courses").header("Authorization", bearer("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotCreateCourses() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer("STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCourse())))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanCreateCourses() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCourse())))
                .andExpect(status().isCreated());
    }

    // ── enrollments ─────────────────────────────────────────────────

    @Test
    void studentCannotListAllEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments").header("Authorization", bearer("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCanLookUpTheirOwnEnrollments() throws Exception {
        long ownId = linkedStudentFor("owner-user", "owner.authz@example.com");

        mockMvc.perform(get("/api/enrollments/student/" + ownId)
                        .header("Authorization", bearerFor("owner-user", "STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotLookUpAnotherStudentsEnrollments() throws Exception {
        long ownId = linkedStudentFor("owner2-user", "owner2.authz@example.com");
        long someoneElse = ownId + 999;

        mockMvc.perform(get("/api/enrollments/student/" + someoneElse)
                        .header("Authorization", bearerFor("owner2-user", "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentWithNoLinkedRecordCannotListEnrollmentsByStudentId() throws Exception {
        // "student-user" has a valid token but no account/student row behind it.
        mockMvc.perform(get("/api/enrollments/student/1").header("Authorization", bearer("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentCannotDeleteEnrollments() throws Exception {
        mockMvc.perform(delete("/api/enrollments/1").header("Authorization", bearer("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotDeleteEnrollments() throws Exception {
        mockMvc.perform(delete("/api/enrollments/1").header("Authorization", bearer("TEACHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanListAllEnrollments() throws Exception {
        mockMvc.perform(get("/api/enrollments").header("Authorization", bearer("TEACHER")))
                .andExpect(status().isOk());
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

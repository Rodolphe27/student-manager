package com.student_manager.shared.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student_manager.feature.course.Course;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.course.CourseStatus;
import com.student_manager.feature.enrollment.Enrollment;
import com.student_manager.feature.enrollment.EnrollmentRepository;
import com.student_manager.feature.enrollment.EnrollmentStatus;
import com.student_manager.feature.enrollment.Grade;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks for the Spring Data patterns behind the list and edit
 * endpoints: pagination, Specification search/filters, projections, auditing,
 * and optimistic locking. Fixtures use a unique marker so counts stay exact
 * even when the shared dev database already holds data; every test rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SpringDataPatternsTest {

    private static final String MARK = "zqxpattern";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private StudentRepository studentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;

    private RequestPostProcessor admin() {
        RequestPostProcessor principal = user("admin-user").roles("ADMIN");
        return request -> csrf().postProcessRequest(principal.postProcessRequest(request));
    }

    private Student student(int i) {
        Student s = new Student();
        s.setFirstName("Ada" + i);
        s.setLastName(MARK + i);
        s.setMatriculationNumber("M-" + MARK + "-" + i);
        s.setEmail(MARK + i + "@example.com");
        return studentRepository.save(s);
    }

    private Course course(String code, CourseStatus status) {
        Course c = new Course();
        c.setCode(code);
        c.setTitle(MARK + " course " + code);
        c.setDescription("not part of the options projection");
        c.setCreditHours(5);
        c.setStatus(status);
        return courseRepository.save(c);
    }

    // ── Pagination + Specification search ────────────────────────────

    @Test
    void studentListIsPagedAndSearchable() throws Exception {
        for (int i = 0; i < 12; i++) {
            student(i);
        }

        mockMvc.perform(get("/api/students").param("q", MARK).param("size", "5").param("page", "1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(12))
                .andExpect(jsonPath("$.page.totalPages").value(3));

        // Search is case-insensitive and also matches the email / matriculation number.
        mockMvc.perform(get("/api/students").param("q", (MARK + "11@EXAMPLE").toUpperCase()).with(admin()))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value(MARK + "11"));
    }

    @Test
    void pageSizeIsCappedAndUnknownSortPropertyIs400() throws Exception {
        mockMvc.perform(get("/api/students").param("size", "5000").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));

        mockMvc.perform(get("/api/students").param("sort", "noSuchField").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property 'noSuchField'"));
    }

    @Test
    void courseListFiltersByTextAndStatus() throws Exception {
        course("ZQX-1", CourseStatus.ACTIVE);
        course("ZQX-2", CourseStatus.ACTIVE);
        course("ZQX-3", CourseStatus.ARCHIVED);

        mockMvc.perform(get("/api/courses").param("q", MARK).with(admin()))
                .andExpect(jsonPath("$.page.totalElements").value(3));
        mockMvc.perform(get("/api/courses").param("q", MARK).param("status", "ACTIVE").with(admin()))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    void enrollmentListFiltersByStatusStudentAndCourse() throws Exception {
        Student s = student(1);
        Course a = course("ZQX-A", CourseStatus.ACTIVE);
        Course b = course("ZQX-B", CourseStatus.ACTIVE);
        for (Object[] row : new Object[][]{{a, EnrollmentStatus.PENDING}, {b, EnrollmentStatus.CONFIRMED}}) {
            Enrollment e = new Enrollment();
            e.setStudent(s);
            e.setCourse((Course) row[0]);
            e.setEnrolledAt(LocalDate.now());
            e.setStatus((EnrollmentStatus) row[1]);
            e.setGrade(Grade.NOT_GRADED);
            enrollmentRepository.save(e);
        }

        mockMvc.perform(get("/api/enrollments").param("studentId", s.getId().toString()).with(admin()))
                .andExpect(jsonPath("$.page.totalElements").value(2));
        mockMvc.perform(get("/api/enrollments")
                        .param("studentId", s.getId().toString())
                        .param("status", "PENDING")
                        .with(admin()))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].courseCode").value("ZQX-A"));
        mockMvc.perform(get("/api/enrollments").param("courseId", b.getId().toString()).with(admin()))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    // ── Projections ──────────────────────────────────────────────────

    @Test
    void optionEndpointsReturnOnlyTheProjectedFields() throws Exception {
        Student s = student(7);
        course("ZQX-OPT", CourseStatus.ACTIVE);

        String students = mockMvc.perform(get("/api/students/options").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode studentOption = findById(objectMapper.readTree(students), s.getId());
        assertThat(studentOption.get("fullName").asText()).isEqualTo("Ada7 " + MARK + "7");
        assertThat(fieldNames(studentOption)).containsExactlyInAnyOrder("id", "fullName", "matriculationNumber");

        String courses = mockMvc.perform(get("/api/courses/options").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode courseOption = null;
        for (JsonNode n : objectMapper.readTree(courses)) {
            if ("ZQX-OPT".equals(n.get("code").asText())) courseOption = n;
        }
        assertThat(courseOption).isNotNull();
        assertThat(fieldNames(courseOption)).containsExactlyInAnyOrder("id", "code", "title", "status");
    }

    // ── Auditing + optimistic locking ────────────────────────────────

    @Test
    void writesRecordWhoCreatedAndLastChangedARow() throws Exception {
        String created = mockMvc.perform(post("/api/students")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Grace", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        Student saved = studentRepository.findById(id).orElseThrow();
        assertThat(saved.getCreatedBy()).isEqualTo("admin-user");
        assertThat(saved.getUpdatedBy()).isEqualTo("admin-user");

        RequestPostProcessor otherAdmin = request -> csrf().postProcessRequest(
                user("second-admin").roles("ADMIN").postProcessRequest(request));
        mockMvc.perform(put("/api/students/" + id)
                        .with(otherAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Grace B.", null)))
                .andExpect(status().isOk());

        Student updated = studentRepository.findById(id).orElseThrow();
        assertThat(updated.getCreatedBy()).isEqualTo("admin-user");
        assertThat(updated.getUpdatedBy()).isEqualTo("second-admin");
    }

    @Test
    void anUpdateBasedOnAStaleVersionIsRejectedWith409() throws Exception {
        String created = mockMvc.perform(post("/api/students")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Grace", null)))
                .andReturn().getResponse().getContentAsString();
        JsonNode dto = objectMapper.readTree(created);
        long id = dto.get("id").asLong();
        long v0 = dto.get("version").asLong();

        // First editor saves with the version they loaded → OK, version moves on.
        mockMvc.perform(put("/api/students/" + id)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("First edit", v0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(v0 + 1));

        // Second editor still holds v0 → conflict, their stale copy must not overwrite.
        mockMvc.perform(put("/api/students/" + id)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("Stale edit", v0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This record was changed by someone else. Reload it and try again."));

        assertThat(studentRepository.findById(id).orElseThrow().getFirstName()).isEqualTo("First edit");
    }

    private String studentJson(String firstName, Long version) {
        return """
                {"firstName":"%s","lastName":"%s","matriculationNumber":"M-%s-AUD","email":"%s.aud@example.com"%s}
                """.formatted(firstName, MARK, MARK, MARK, version == null ? "" : ",\"version\":" + version);
    }

    private static JsonNode findById(JsonNode array, long id) {
        for (JsonNode n : array) {
            if (n.get("id").asLong() == id) return n;
        }
        throw new AssertionError("id " + id + " not in response");
    }

    private static java.util.List<String> fieldNames(JsonNode node) {
        java.util.List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}

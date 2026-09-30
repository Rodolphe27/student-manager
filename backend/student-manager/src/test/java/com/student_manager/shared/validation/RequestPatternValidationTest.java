package com.student_manager.shared.validation;

import com.student_manager.feature.auth.RegisterRequest;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.course.CreateCourseRequest;
import com.student_manager.feature.student.CreateStudentRequest;
import com.student_manager.feature.teacher.CreateTeacherRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@code @Pattern} constraints on request DTOs, exercised
 * directly through Jakarta Bean Validation — no Spring context or HTTP layer
 * needed, so these run fast and pin down exactly what each regex accepts/rejects.
 */
class RequestPatternValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    // ── RegisterRequest.username ───────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "ab",                                       // too short (min 3)
            "user name",                                 // space not allowed
            "user@name",                                  // '@' not allowed
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",   // 40 chars, too long (max 32)
    })
    void invalidUsernamesAreRejected(String username) {
        RegisterRequest request = new RegisterRequest(username, "user@example.com", "Password123", Role.STUDENT);
        assertThat(fieldViolations(request, "username")).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainuser", "user.name", "user_name", "user-name", "abc"})
    void validUsernamesAreAccepted(String username) {
        RegisterRequest request = new RegisterRequest(username, "user@example.com", "Password123", Role.STUDENT);
        assertThat(fieldViolations(request, "username")).isEmpty();
    }

    // ── RegisterRequest.password ────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {
            "short1",        // too short (min 8)
            "alllowercase",  // no digit
            "12345678",      // no letter
            "NoDigitsHere",  // no digit
    })
    void weakPasswordsAreRejected(String password) {
        RegisterRequest request = new RegisterRequest("plainuser", "user@example.com", password, Role.STUDENT);
        assertThat(fieldViolations(request, "password")).isNotEmpty();
    }

    @Test
    void aPasswordWithALetterAndADigitAndEightCharsIsAccepted() {
        RegisterRequest request = new RegisterRequest("plainuser", "user@example.com", "Password123", Role.STUDENT);
        assertThat(fieldViolations(request, "password")).isEmpty();
    }

    // ── CreateCourseRequest.code ─────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"CS 101", "CS--101", "-CS101", "CS101-"})
    void malformedCourseCodesAreRejected(String code) {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setCode(code);
        request.setTitle("Intro");
        request.setCreditHours(3);
        assertThat(fieldViolations(request, "code")).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CS101", "CS-101", "AUTHZ-ENR-1"})
    void wellFormedCourseCodesAreAccepted(String code) {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setCode(code);
        request.setTitle("Intro");
        request.setCreditHours(3);
        assertThat(fieldViolations(request, "code")).isEmpty();
    }

    // ── CreateStudentRequest.matriculationNumber ─────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"M", "has space"})
    void malformedMatriculationNumbersAreRejected(String value) {
        CreateStudentRequest request = new CreateStudentRequest();
        request.setFirstName("A");
        request.setLastName("B");
        request.setEmail("a@example.com");
        request.setMatriculationNumber(value);
        assertThat(fieldViolations(request, "matriculationNumber")).isNotEmpty();
    }

    @Test
    void aWellFormedMatriculationNumberIsAccepted() {
        CreateStudentRequest request = new CreateStudentRequest();
        request.setFirstName("A");
        request.setLastName("B");
        request.setEmail("a@example.com");
        request.setMatriculationNumber("M-AUTHZ-1");
        assertThat(fieldViolations(request, "matriculationNumber")).isEmpty();
    }

    // ── CreateTeacherRequest.department (optional) ───────────────────

    @Test
    void aNullDepartmentIsValid() {
        CreateTeacherRequest request = new CreateTeacherRequest();
        request.setFirstName("A");
        request.setLastName("B");
        request.setEmail("a@example.com");
        assertThat(fieldViolations(request, "department")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CS101", "Dept_1"})
    void aDepartmentWithDigitsOrUnderscoresIsRejected(String department) {
        CreateTeacherRequest request = new CreateTeacherRequest();
        request.setFirstName("A");
        request.setLastName("B");
        request.setEmail("a@example.com");
        request.setDepartment(department);
        assertThat(fieldViolations(request, "department")).isNotEmpty();
    }

    @Test
    void aFreeformDepartmentNameIsAccepted() {
        CreateTeacherRequest request = new CreateTeacherRequest();
        request.setFirstName("A");
        request.setLastName("B");
        request.setEmail("a@example.com");
        request.setDepartment("Computer Science");
        assertThat(fieldViolations(request, "department")).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────

    private <T> Set<ConstraintViolation<T>> fieldViolations(T request, String fieldName) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals(fieldName))
                .collect(Collectors.toSet());
    }
}

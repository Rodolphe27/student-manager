package com.student_manager.shared.security;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.enrollment.EnrollmentRepository;
import com.student_manager.feature.student.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * SpEL-callable authorization helper for {@code @PreAuthorize}. Lets staff
 * (TEACHER / ADMIN) through unconditionally, but restricts a STUDENT to their
 * own student record — closing the IDOR on
 * {@code GET /api/enrollments/student/{studentId}}.
 */
@Slf4j
@Component("ownershipGuard")
@RequiredArgsConstructor
public class OwnershipGuard {

    // Derived from the Role enum, not hand-typed, so a role name typo or rename
    // fails the build here instead of silently locking staff out of this check.
    private static final Set<String> STAFF_AUTHORITIES = Set.of(
            "ROLE_" + Role.TEACHER.name(), "ROLE_" + Role.ADMIN.name());

    private final StudentService studentService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Determines whether the current caller may access a given student's
     * data. Staff (TEACHER/ADMIN) are always allowed; a STUDENT is allowed
     * only for their own record.
     *
     * @param studentId      the id of the student record being accessed
     * @param authentication the caller's authentication, or {@code null} if unauthenticated
     * @return {@code true} if access is allowed
     */
    public boolean canAccessStudentData(Long studentId, Authentication authentication) {
        if (authentication == null || studentId == null) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_AUTHORITIES::contains);
        if (isStaff) {
            return true;
        }

        boolean owns = studentService.accountOwnsStudent(authentication.getName(), studentId);
        if (!owns) {
            log.warn("Blocked cross-student access: {} tried to read student {}",
                    authentication.getName(), studentId);
        }
        return owns;
    }

    /**
     * SecurityConfig already restricts GET /api/enrollments/course/** to TEACHER/ADMIN, so this
     * only needs to further narrow TEACHER to courses they actually teach — ADMIN is unrestricted.
     * A course with no teacher assigned is accessible to ADMIN only (fails closed for TEACHER).
     *
     * @param courseId       the id of the course whose roster is being accessed
     * @param authentication the caller's authentication, or {@code null} if unauthenticated
     * @return {@code true} if access is allowed
     */
    public boolean canAccessCourseData(Long courseId, Authentication authentication) {
        if (authentication == null || courseId == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        if (!courseRepository.existsById(courseId)) {
            // Let a nonexistent id reach the controller so a TEACHER gets the same result
            // (an empty roster) ADMIN would see, instead of a misleading 403.
            return true;
        }

        boolean teaches = courseRepository.existsByIdAndTeacher_Account_Username(courseId, authentication.getName());
        if (!teaches) {
            log.warn("Blocked cross-course access: {} tried to read course {} roster",
                    authentication.getName(), courseId);
        }
        return teaches;
    }

    /**
     * SecurityConfig already restricts the enrollment confirm/cancel/grade endpoints to
     * TEACHER/ADMIN, so this only needs to further narrow TEACHER to enrollments in courses
     * they actually teach — ADMIN is unrestricted. Mirrors {@link #canAccessCourseData}, but
     * resolves the course through the enrollment id since these endpoints are keyed by
     * enrollment, not course. Shared by confirm, cancel, and grade — all three have identical
     * "must teach this course" access rules.
     *
     * @param enrollmentId   the id of the enrollment being confirmed/cancelled/graded
     * @param authentication the caller's authentication, or {@code null} if unauthenticated
     * @return {@code true} if access is allowed
     */
    public boolean canManageEnrollment(Long enrollmentId, Authentication authentication) {
        if (authentication == null || enrollmentId == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        if (!enrollmentRepository.existsById(enrollmentId)) {
            // Let a nonexistent id reach the controller so a TEACHER gets the same 404
            // (ResourceNotFoundException) ADMIN would see, instead of a misleading 403.
            return true;
        }

        boolean teaches = enrollmentRepository.existsByIdAndCourse_Teacher_Account_Username(
                enrollmentId, authentication.getName());
        if (!teaches) {
            log.warn("Blocked cross-course enrollment management: {} tried to act on enrollment {}",
                    authentication.getName(), enrollmentId);
        }
        return teaches;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(("ROLE_" + Role.ADMIN.name())::equals);
    }
}

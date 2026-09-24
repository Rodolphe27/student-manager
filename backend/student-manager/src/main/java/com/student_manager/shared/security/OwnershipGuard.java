package com.student_manager.shared.security;

import com.student_manager.feature.auth.Role;
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
}

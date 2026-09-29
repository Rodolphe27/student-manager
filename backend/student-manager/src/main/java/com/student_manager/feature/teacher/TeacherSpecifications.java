package com.student_manager.feature.teacher;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Search criteria for the paged teacher list ({@code GET /api/teachers?q=}).
 */
final class TeacherSpecifications {

    private TeacherSpecifications() {
    }

    /**
     * Case-insensitive "contains" match on first name, last name, email, or
     * department. A blank query matches every teacher.
     *
     * @param query the search text, may be {@code null}
     * @return the search criteria
     */
    static Specification<Teacher> matching(String query) {
        return (root, criteriaQuery, cb) -> {
            if (!StringUtils.hasText(query)) {
                return null; // no restriction
            }
            String pattern = "%" + query.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("department")), pattern));
        };
    }
}

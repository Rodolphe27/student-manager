package com.student_manager.feature.student;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Search criteria for the paged student list ({@code GET /api/students?q=}).
 */
final class StudentSpecifications {

    private StudentSpecifications() {
    }

    /**
     * Case-insensitive "contains" match on first name, last name,
     * matriculation number, or email. A blank query matches every student.
     *
     * @param query the search text, may be {@code null}
     * @return the search criteria
     */
    static Specification<Student> matching(String query) {
        return (root, criteriaQuery, cb) -> {
            if (!StringUtils.hasText(query)) {
                return null; // no restriction
            }
            String pattern = "%" + query.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("matriculationNumber")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern));
        };
    }
}

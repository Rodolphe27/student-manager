package com.student_manager.feature.course;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Search criteria for the paged course list ({@code GET /api/courses?q=&status=}).
 */
final class CourseSpecifications {

    private CourseSpecifications() {
    }

    /**
     * Case-insensitive "contains" match on code or title, optionally narrowed
     * to one status. Both criteria are optional; with neither, every course matches.
     *
     * @param query  the search text, may be {@code null}
     * @param status the status to keep, may be {@code null}
     * @return the search criteria
     */
    static Specification<Course> matching(String query, CourseStatus status) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query)) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

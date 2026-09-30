package com.student_manager.feature.enrollment;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter criteria for the paged enrollment list
 * ({@code GET /api/enrollments?status=&studentId=&courseId=}).
 */
final class EnrollmentSpecifications {

    private EnrollmentSpecifications() {
    }

    /**
     * Keeps enrollments matching every given filter; {@code null} filters are
     * ignored, so with none set every enrollment matches.
     *
     * @param status    the status to keep, may be {@code null}
     * @param studentId the student to keep, may be {@code null}
     * @param courseId  the course to keep, may be {@code null}
     * @param teacherId keep only enrollments in courses run by this teacher, may be {@code null}
     * @return the filter criteria
     */
    static Specification<Enrollment> filter(EnrollmentStatus status, Long studentId, Long courseId, Long teacherId) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (studentId != null) {
                predicates.add(cb.equal(root.get("student").get("id"), studentId));
            }
            if (courseId != null) {
                predicates.add(cb.equal(root.get("course").get("id"), courseId));
            }
            if (teacherId != null) {
                predicates.add(cb.equal(root.get("course").get("teacher").get("id"), teacherId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

package com.student_manager.feature.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Enrollment}.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Finds all enrollments belonging to a given student.
     *
     * @param studentId the student id
     * @return that student's enrollments
     */
    List<Enrollment> findByStudentId(Long studentId);

    /**
     * Finds all enrollments (the roster) for a given course.
     *
     * @param courseId the course id
     * @return that course's enrollments
     */
    List<Enrollment> findByCourseId(Long courseId);

    /**
     * Finds all enrollments with a given status.
     *
     * @param status the enrollment status to filter by
     * @return enrollments currently in that status
     */
    List<Enrollment> findByStatus(EnrollmentStatus status);

    /**
     * Checks whether a student already has an enrollment row for a course.
     * Used to enforce the one-enrollment-per-student-per-course rule
     * (see {@link Enrollment}'s unique constraint) before inserting a new row.
     *
     * @param studentId the student id
     * @param courseId the course id
     * @return {@code true} if an enrollment already exists for that pair
     */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}

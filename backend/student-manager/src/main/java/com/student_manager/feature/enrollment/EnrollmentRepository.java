package com.student_manager.feature.enrollment;

import com.student_manager.shared.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Enrollment}.
 *
 * <p>The list queries fetch {@code student} and {@code course} eagerly via
 * {@link EntityGraph}: {@code EnrollmentServiceImpl#toDTO} reads both lazy
 * associations for every row, which would otherwise cost two extra queries
 * per enrollment (N+1).
 */
@Repository
public interface EnrollmentRepository extends BaseRepository<Enrollment> {

    /**
     * One page of enrollments matching {@code spec}, with their student and
     * course fetched in the same query (the separate count query is unaffected).
     *
     * @param spec     the filter criteria
     * @param pageable the requested page, size and sort
     * @return the requested page of enrollments
     */
    @Override
    @EntityGraph(attributePaths = {"student", "course"})
    Page<Enrollment> findAll(Specification<Enrollment> spec, Pageable pageable);

    /**
     * Finds all enrollments belonging to a given student.
     *
     * @param studentId the student id
     * @return that student's enrollments
     */
    @EntityGraph(attributePaths = {"student", "course"})
    List<Enrollment> findByStudentId(Long studentId);

    /**
     * Finds all enrollments (the roster) for a given course.
     *
     * @param courseId the course id
     * @return that course's enrollments
     */
    @EntityGraph(attributePaths = {"student", "course"})
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

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /**
     * Checks whether the given account teaches the course this enrollment belongs to.
     * Used by {@code OwnershipGuard} to restrict a TEACHER to grading only their own courses.
     *
     * @param id the enrollment id
     * @param teacherId the teacher profile id to check
     * @return {@code true} if that teacher runs this enrollment's course
     */
    boolean existsByIdAndCourseTeacherId(Long id, Long teacherId);
}

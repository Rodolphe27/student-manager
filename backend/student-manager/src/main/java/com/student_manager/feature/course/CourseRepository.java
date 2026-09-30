package com.student_manager.feature.course;

import com.student_manager.shared.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository providing CRUD, paged search ({@link BaseRepository})
 * and lookup operations for {@link Course} entities.
 */
@Repository
public interface CourseRepository extends BaseRepository<Course> {

    /**
     * Every course as a {@link CourseOption}, for dropdowns. An interface-based
     * (closed) projection on a derived query: Spring Data selects only the
     * columns behind the interface's getters ("ProjectedBy" is just a readable
     * name — the query has no criteria).
     *
     * @param sort the ordering, e.g. by code
     * @return all courses as options
     */
    List<CourseOption> findAllProjectedBy(Sort sort);

    /**
     * The courses run by one teacher as {@link CourseOption}s.
     *
     * @param teacherId the teacher profile id
     * @param sort the ordering, e.g. by code
     * @return that teacher's courses as options
     */
    List<CourseOption> findProjectedByTeacherId(Long teacherId, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"teacher", "term"})
    Page<Course> findAll(Specification<Course> spec, Pageable pageable);

    /**
     * Finds a course by its unique code.
     *
     * @param code the course code to look up
     * @return the matching course, or empty if none exists
     */
    Optional<Course> findByCode(String code);

    /**
     * Checks whether a course with the given code already exists.
     *
     * @param code the course code to check
     * @return {@code true} if a course with that code exists
     */
    boolean existsByCode(String code);

    /**
     * Checks whether a course code is already used by a course other than the given one.
     * Used to validate uniqueness on update without conflicting with the record being updated.
     *
     * @param code the course code to check
     * @param id the id of the course being excluded from the check
     * @return {@code true} if another course already uses that code
     */
    boolean existsByCodeAndIdNot(String code, Long id);

    /**
     * Finds all courses with the given status.
     *
     * @param status the status to filter by
     * @return the matching courses
     */
    @EntityGraph(attributePaths = {"teacher", "term"})
    List<Course> findByStatus(CourseStatus status);

    /**
     * Used by OwnershipGuard to check whether the given teacher runs this course.
     *
     * @param id the course id
     * @param teacherId the teacher profile id to check
     * @return {@code true} if the course exists and is taught by that teacher
     */
    boolean existsByIdAndTeacherId(Long id, Long teacherId);
}

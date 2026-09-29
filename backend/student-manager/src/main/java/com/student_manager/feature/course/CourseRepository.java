package com.student_manager.feature.course;

import com.student_manager.shared.repository.BaseRepository;
import org.springframework.data.domain.Sort;
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
    List<Course> findByStatus(CourseStatus status);

    /**
     * Used by OwnershipGuard to check whether the given account teaches this course.
     *
     * @param id the course id
     * @param username the account username to check ownership for
     * @return {@code true} if the course exists and is taught by the given account
     */
    boolean existsByIdAndTeacher_Account_Username(Long id, String username);
}

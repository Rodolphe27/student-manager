package com.student_manager.feature.teacher;

import com.student_manager.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository providing CRUD, paged search ({@link BaseRepository})
 * and lookup operations for {@link Teacher} entities.
 */
public interface TeacherRepository extends BaseRepository<Teacher> {

    /**
     * Every teacher as a {@link TeacherOption}, ordered by name, for dropdowns.
     *
     * @return all teachers as id + full name + department
     */
    @Query("""
            select new com.student_manager.feature.teacher.TeacherOption(
                t.id, concat(t.firstName, ' ', t.lastName), t.department)
            from Teacher t
            order by t.lastName, t.firstName
            """)
    List<TeacherOption> findAllOptions();

    /**
     * Finds a teacher by their unique email address.
     *
     * @param email the email to look up
     * @return the matching teacher, or empty if none exists
     */
    Optional<Teacher> findByEmail(String email);

    /**
     * Checks whether a teacher with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if a teacher with that email exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether an email is already used by a teacher other than the given one.
     * Used to validate uniqueness on update without conflicting with the record being updated.
     *
     * @param email the email to check
     * @param id the id of the teacher being excluded from the check
     * @return {@code true} if another teacher already uses that email
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Finds the teacher profile linked to the given user account username.
     *
     * @param username the linked account's username
     * @return the matching teacher, or empty if no teacher profile is linked to that username
     */
    Optional<Teacher> findByAccountUsername(String username);
}

package com.student_manager.feature.student;

import com.student_manager.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Student}. Paged search comes from
 * {@link BaseRepository} ({@code findAll(Specification, Pageable)}).
 */
public interface StudentRepository extends BaseRepository<Student> {

    /**
     * Every student as a lightweight {@link StudentOption}, for dropdowns.
     * A JPQL constructor-expression projection: only the three needed columns
     * are selected (no full entities loaded), and the display name is built
     * in the query.
     *
     * @return all students, ordered by last then first name
     */
    @Query("""
            select new com.student_manager.feature.student.StudentOption(
                s.id, concat(s.firstName, ' ', s.lastName), s.matriculationNumber)
            from Student s
            order by s.lastName, s.firstName
            """)
    List<StudentOption> findAllOptions();

    /**
     * Finds the student with the given email.
     *
     * @param email the email to search for
     * @return the matching student, if any
     */
    Optional<Student> findByEmail(String email);

    /**
     * Finds the student with the given matriculation number.
     *
     * @param matriculationNumber the matriculation number to search for
     * @return the matching student, if any
     */
    Optional<Student> findByMatriculationNumber(String matriculationNumber);

    /**
     * Checks whether a student with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if a student has that email
     */
    boolean existsByEmail(String email);

    /**
     * Checks whether a student with the given matriculation number already exists.
     *
     * @param matriculationNumber the matriculation number to check
     * @return {@code true} if a student has that matriculation number
     */
    boolean existsByMatriculationNumber(String matriculationNumber);

    /**
     * Checks whether a student other than the given id already uses this email.
     * Used when updating a student to avoid colliding with a different student's email.
     *
     * @param email the email to check
     * @param id the id to exclude from the check
     * @return {@code true} if some other student has that email
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Checks whether a student other than the given id already uses this
     * matriculation number. Used when updating a student to avoid colliding
     * with a different student's matriculation number.
     *
     * @param matriculationNumber the matriculation number to check
     * @param id the id to exclude from the check
     * @return {@code true} if some other student has that matriculation number
     */
    boolean existsByMatriculationNumberAndIdNot(String matriculationNumber, Long id);

    /**
     * Finds the student whose {@code account} FK points at the login account
     * with the given username.
     *
     * @param username the linked account's username
     * @return the matching student, if any
     */
    Optional<Student> findByAccountUsername(String username);
}

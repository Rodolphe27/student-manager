package com.student_manager.feature.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Student}.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

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

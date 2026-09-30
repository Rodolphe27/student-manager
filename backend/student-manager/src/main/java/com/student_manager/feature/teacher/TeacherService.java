package com.student_manager.feature.teacher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service contract for managing {@link Teacher} resources, covering lookup,
 * creation, update and deletion.
 */
public interface TeacherService {

    /**
     * Retrieves a single teacher by id.
     *
     * @param id the teacher id
     * @return the matching teacher as a DTO
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no teacher exists with the given id
     */
    TeacherDTO findById(Long id);

    /**
     * Returns one page of teachers, optionally filtered by a search text
     * matched against name, email and department.
     *
     * @param query    the search text, or {@code null}/blank for all teachers
     * @param pageable the requested page, size and sort
     * @return the requested page of teachers
     */
    Page<TeacherDTO> search(String query, Pageable pageable);

    /**
     * Lists every teacher as a lightweight {@link TeacherOption}, for selection lists.
     *
     * @return all teachers, ordered by name
     */
    List<TeacherOption> options();

    /**
     * Resolves the id of the teacher profile linked to an account: prefers the
     * {@code account} FK, falling back to matching the account's e-mail against the
     * teacher's e-mail (the same rule as for students). Used by authorization checks so a
     * TEACHER can only manage the courses they run. Never throws.
     *
     * @param username the account's username
     * @return the teacher's id, or empty if the account has no teacher profile
     */
    Optional<Long> findIdByAccountUsername(String username);

    /**
     * Creates a new teacher profile.
     *
     * @param request the data for the teacher to create
     * @return the newly created teacher
     * @throws com.student_manager.shared.exception.ValidationException if the email is already in use
     */
    TeacherDTO create(CreateTeacherRequest request);

    /**
     * Updates an existing teacher profile.
     *
     * @param id the id of the teacher to update
     * @param request the replacement teacher data
     * @return the updated teacher
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no teacher exists with the given id
     * @throws com.student_manager.shared.exception.ValidationException if the new email is already used by another teacher
     */
    TeacherDTO update(Long id, CreateTeacherRequest request);

    /**
     * Deletes a teacher profile.
     *
     * @param id the id of the teacher to delete
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no teacher exists with the given id
     */
    void delete(Long id);
}

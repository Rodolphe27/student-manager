package com.student_manager.feature.teacher;

import java.util.List;

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
     * Retrieves every teacher in the system.
     *
     * @return the list of all teachers
     */
    List<TeacherDTO> findAll();

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

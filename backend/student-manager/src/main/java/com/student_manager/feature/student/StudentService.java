package com.student_manager.feature.student;

import java.util.List;

/**
 * Business operations for managing student profiles: CRUD plus the
 * account-linkage lookups used for self-service access and authorization.
 */
public interface StudentService {

    /**
     * Looks up a single student by id.
     *
     * @param id the student id
     * @return the matching student
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student has that id
     */
    StudentDTO findById(Long id);

    /**
     * Resolves the student record linked to an authenticated account: prefers
     * the {@code account} FK set by a claimed {@code RegistrationInvite}, falling
     * back to matching the account's e-mail against {@link Student#getEmail()}
     * for students who predate the invite flow. Used by {@code GET
     * /api/students/me} so a STUDENT can see their own record without being able
     * to read the whole roster.
     */
    StudentDTO findByAccountUsername(String username);

    /**
     * True when the account identified by {@code username} resolves (via the
     * same invite-FK-then-email lookup as {@link #findByAccountUsername}) to the
     * student row {@code studentId}. Used by authorization checks so a STUDENT
     * can only reach their own data. Never throws — a missing account or student
     * row simply yields {@code false}.
     */
    boolean accountOwnsStudent(String username, Long studentId);

    /**
     * Lists every student in the system.
     *
     * @return all students
     */
    List<StudentDTO> findAll();

    /**
     * Creates a new student profile.
     *
     * @param request the student's details
     * @return the created student
     * @throws com.student_manager.shared.exception.ValidationException if the email or matriculation number is already in use
     */
    StudentDTO create(CreateStudentRequest request);

    /**
     * Updates an existing student's details.
     *
     * @param id the student id
     * @param request the new details
     * @return the updated student
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student has that id
     * @throws com.student_manager.shared.exception.ValidationException if the email or matriculation number is already used by another student
     */
    StudentDTO update(Long id, CreateStudentRequest request);

    /**
     * Deletes a student profile.
     *
     * @param id the student id
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student has that id
     */
    void delete(Long id);
}

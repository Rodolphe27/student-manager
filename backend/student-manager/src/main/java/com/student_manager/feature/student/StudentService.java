package com.student_manager.feature.student;

import java.util.List;

public interface StudentService {
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

    List<StudentDTO> findAll();
    StudentDTO create(CreateStudentRequest request);
    StudentDTO update(Long id, CreateStudentRequest request);
    void delete(Long id);
}

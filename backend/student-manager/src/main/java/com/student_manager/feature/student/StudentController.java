package com.student_manager.feature.student;

import com.student_manager.feature.auth.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Per-method entry logging removed — RequestLoggingFilter (shared/config) now
// logs method + path + status + duration for every request. The lines below
// are commented out, not deleted, for reference.
/**
 * REST endpoints for managing student profiles: CRUD operations, the
 * self-service "my profile" lookup.
 */
@Slf4j
@RestController
// TODO(SEC-12) [LOW]: no API versioning — see AuthController.
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    /**
     * Returns one page of students, optionally filtered by {@code q} (name,
     * matriculation number, or email). Default order: last name, first name.
     *
     * @param q        optional search text
     * @param pageable {@code page} (0-based), {@code size} (max 100) and {@code sort} query params
     * @return the requested page of students
     */
    @GetMapping
    public ResponseEntity<Page<StudentDTO>> getAll(
            @RequestParam(required = false) String q,
            @PageableDefault(sort = {"lastName", "firstName"}) Pageable pageable) {
        return ResponseEntity.ok(service.search(q, pageable));
    }

    /**
     * Every student as a lightweight id/name/matriculation option, for
     * selection lists such as the enrollment form.
     *
     * @return all students, ordered by name
     */
    @GetMapping("options")
    public ResponseEntity<List<StudentOption>> getOptions() {
        return ResponseEntity.ok(service.options());
    }

    /**
     * Returns the student profile linked to the currently authenticated
     * account, so a STUDENT can see their own record without needing
     * permission to read the whole roster.
     *
     * @param authentication the current caller's authentication
     * @return the caller's own student record
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student record is linked to the account
     */
    @GetMapping("me")
    public ResponseEntity<StudentDTO> getMe(Authentication authentication) {
        // log.info("GET /api/students/me ({})", authentication.getName());
        return ResponseEntity.ok(service.findByAccountUsername(authentication.getName()));
    }

    /**
     * Looks up a single student by id.
     *
     * @param id the student id
     * @return the matching student
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student has that id
     */
    @GetMapping("{id}")
    public ResponseEntity<StudentDTO> getById(@PathVariable Long id) {
        // log.info("GET /api/students/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Creates a new student profile.
     *
     * @param request the student's details
     * @return the created student, with HTTP 201
     * @throws com.student_manager.shared.exception.ValidationException if the email or matriculation number is already in use
     */
    @PostMapping
    public ResponseEntity<StudentDTO> create(@Valid @RequestBody CreateStudentRequest request) {
        // log.info("POST /api/students");
        return ResponseEntity.status(201).body(service.create(request));
    }

    /**
     * Updates an existing student's details.
     *
     * @param id the student id
     * @param request the new details
     * @return the updated student
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student has that id
     * @throws com.student_manager.shared.exception.ValidationException if the email or matriculation number is already used by another student
     */
    @PutMapping("{id}")
    public ResponseEntity<StudentDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody CreateStudentRequest request) {
        // log.info("PUT /api/students/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    /**
     * Deletes a student profile.
     *
     * @param id the student id
     * @return HTTP 204 with no body
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student has that id
     */
    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // log.info("DELETE /api/students/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

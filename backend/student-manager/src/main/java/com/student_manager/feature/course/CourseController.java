package com.student_manager.feature.course;

import com.student_manager.shared.security.OwnershipGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Per-method entry logging removed — RequestLoggingFilter (shared/config) now
// logs method + path + status + duration for every request. The lines below
// are commented out, not deleted, for reference.
/**
 * REST controller exposing CRUD endpoints for {@link Course} resources under
 * {@code /api/courses}. Per the security configuration, {@code GET} requests
 * only require an authenticated user, while all other methods require the
 * {@code TEACHER} or {@code ADMIN} role.
 */
@Slf4j
@RestController
// TODO(SEC-12) [LOW]: no API versioning — see AuthController.
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService service;
    private final OwnershipGuard ownershipGuard;

    /**
     * Returns one page of courses, optionally filtered by {@code q} (code or
     * title) and/or {@code status}. Default order: course code.
     *
     * @param q        optional search text
     * @param status   optional status filter
     * @param pageable {@code page} (0-based), {@code size} (max 100) and {@code sort} query params
     * @return 200 OK with the requested page of courses
     */
    @GetMapping
    public ResponseEntity<Page<CourseDTO>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CourseStatus status,
            @PageableDefault(sort = "code") Pageable pageable) {
        return ResponseEntity.ok(service.search(q, status, pageable));
    }

    /**
     * Every course as a lightweight id/code/title option, for selection lists
     * such as the enrollment form.
     *
     * @param authentication the caller; a TEACHER only gets the courses they run
     * @return 200 OK with the courses, ordered by code
     */
    @GetMapping("options")
    public ResponseEntity<List<CourseOption>> getOptions(Authentication authentication) {
        return ResponseEntity.ok(service.options(ownershipGuard.teacherScope(authentication)));
    }

    /**
     * Retrieves a single course by its id.
     *
     * @param id the course id
     * @return 200 OK with the matching course
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no course exists with the given id
     */
    @GetMapping("{id}")
    public ResponseEntity<CourseDTO> getById(@PathVariable Long id) {
        // log.info("GET /api/courses/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Lists all courses that currently have the given status.
     *
     * @param status the status to filter by
     * @return 200 OK with the matching courses
     */
    @GetMapping("status/{status}")
    public ResponseEntity<List<CourseDTO>> getByStatus(@PathVariable CourseStatus status) {
        // log.info("GET /api/courses/status/{}", status);
        return ResponseEntity.ok(service.findByStatus(status));
    }

    /**
     * Creates a new course. A TEACHER always becomes the course's teacher; an ADMIN picks one
     * (or none) via {@code teacherId}.
     *
     * @param request the validated course data to create
     * @param authentication the caller
     * @return 201 Created with the newly created course
     * @throws com.student_manager.shared.exception.ValidationException if the course code is already in use
     */
    @PostMapping
    public ResponseEntity<CourseDTO> create(@Valid @RequestBody CreateCourseRequest request,
                                            Authentication authentication) {
        pinTeacher(request, authentication);
        return ResponseEntity.status(201).body(service.create(request));
    }

    /**
     * Updates an existing course. A TEACHER may only change courses they run (and cannot hand
     * them to someone else); an ADMIN may change any.
     *
     * @param id the id of the course to update
     * @param request the validated replacement course data
     * @param authentication the caller
     * @return 200 OK with the updated course
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no course exists with the given id
     * @throws com.student_manager.shared.exception.ValidationException if the new course code is already used by another course
     */
    @PutMapping("{id}")
    @PreAuthorize("@ownershipGuard.canManageCourse(#id, authentication)")
    public ResponseEntity<CourseDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody CreateCourseRequest request,
                                             Authentication authentication) {
        pinTeacher(request, authentication);
        return ResponseEntity.ok(service.update(id, request));
    }

    /**
     * Deletes a course.
     *
     * @param id the id of the course to delete
     * @return 204 No Content on success
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no course exists with the given id
     */
    @DeleteMapping("{id}")
    @PreAuthorize("@ownershipGuard.canManageCourse(#id, authentication)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // log.info("DELETE /api/courses/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** A TEACHER can only create/keep courses for themselves: force {@code teacherId} to their own. */
    private void pinTeacher(CreateCourseRequest request, Authentication authentication) {
        Long scope = ownershipGuard.teacherScope(authentication);
        if (scope == null) {
            return;
        }
        if (scope == OwnershipGuard.NO_TEACHER) {
            throw new AccessDeniedException("Your account is not linked to a teacher profile");
        }
        request.setTeacherId(scope);
    }
}

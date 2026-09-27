package com.student_manager.feature.course;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    /**
     * Lists every course in the system.
     *
     * @return 200 OK with the list of all courses
     */
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAll() {
        // log.info("GET /api/courses");
        return ResponseEntity.ok(service.findAll());
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
     * Creates a new course.
     *
     * @param request the validated course data to create
     * @return 201 Created with the newly created course
     * @throws com.student_manager.shared.exception.ValidationException if the course code is already in use
     */
    @PostMapping
    public ResponseEntity<CourseDTO> create(@Valid @RequestBody CreateCourseRequest request) {
        // log.info("POST /api/courses");
        return ResponseEntity.status(201).body(service.create(request));
    }

    /**
     * Updates an existing course.
     *
     * @param id the id of the course to update
     * @param request the validated replacement course data
     * @return 200 OK with the updated course
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no course exists with the given id
     * @throws com.student_manager.shared.exception.ValidationException if the new course code is already used by another course
     */
    @PutMapping("{id}")
    public ResponseEntity<CourseDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody CreateCourseRequest request) {
        // log.info("PUT /api/courses/{}", id);
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // log.info("DELETE /api/courses/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

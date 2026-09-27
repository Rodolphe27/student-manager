package com.student_manager.feature.enrollment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Per-method entry logging removed — RequestLoggingFilter (shared/config) now
// logs method + path + status + duration for every request. The lines below
// are commented out, not deleted, for reference.
/**
 * REST endpoints for managing enrollments: creating them, listing them by
 * student/course, and transitioning an enrollment through confirm/cancel/grade.
 */
@Slf4j
@RestController
// TODO(SEC-12) [LOW]: no API versioning — see AuthController.
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService service;

    /**
     * Lists every enrollment in the system.
     *
     * @return all enrollments
     */
    @GetMapping
    public ResponseEntity<List<EnrollmentDTO>> getAll() {
        // log.info("GET /api/enrollments");
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * Looks up a single enrollment by id.
     *
     * @param id the enrollment id
     * @return the matching enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     */
    @GetMapping("{id}")
    public ResponseEntity<EnrollmentDTO> getById(@PathVariable Long id) {
        // log.info("GET /api/enrollments/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Lists all enrollments for a given student. Access is restricted by
     * {@code @ownershipGuard}: staff (TEACHER/ADMIN) may read any student's
     * enrollments, but a STUDENT may only read their own.
     *
     * @param studentId the student id
     * @return the student's enrollments
     */
    @GetMapping("student/{studentId}")
    @PreAuthorize("@ownershipGuard.canAccessStudentData(#studentId, authentication)")
    public ResponseEntity<List<EnrollmentDTO>> getByStudent(@PathVariable Long studentId) {
        // log.info("GET /api/enrollments/student/{}", studentId);
        return ResponseEntity.ok(service.findByStudentId(studentId));
    }

    /**
     * Lists all enrollments (the roster) for a given course. Access is
     * restricted by {@code @ownershipGuard}: ADMIN may read any course's
     * roster, but a TEACHER may only read rosters for courses they teach.
     *
     * @param courseId the course id
     * @return the course's enrollments
     */
    @GetMapping("course/{courseId}")
    @PreAuthorize("@ownershipGuard.canAccessCourseData(#courseId, authentication)")
    public ResponseEntity<List<EnrollmentDTO>> getByCourse(@PathVariable Long courseId) {
        // log.info("GET /api/enrollments/course/{}", courseId);
        return ResponseEntity.ok(service.findByCourseId(courseId));
    }

    // SecurityConfig lets STUDENT/TEACHER/ADMIN all reach this endpoint; this
    // check is what stops a student enrolling anyone but themselves — staff
    // pass through unconditionally (see OwnershipGuard).
    /**
     * Enrolls a student in a course. A STUDENT caller may only enroll
     * themselves; staff may enroll any student.
     *
     * @param request the student/course pair to enroll
     * @return the created enrollment, with HTTP 201
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if the student or course does not exist
     * @throws com.student_manager.shared.exception.ValidationException if the course is inactive or the student is already enrolled in it
     */
    @PostMapping
    @PreAuthorize("@ownershipGuard.canAccessStudentData(#request.studentId, authentication)")
    public ResponseEntity<EnrollmentDTO> create(
            @Valid @RequestBody CreateEnrollmentRequest request) {
        // log.info("POST /api/enrollments");
        return ResponseEntity.status(201).body(service.create(request));
    }

    /**
     * Confirms a pending enrollment. Access is restricted by
     * {@code @ownershipGuard}: ADMIN may confirm any enrollment, but a TEACHER
     * may only confirm enrollments in courses they teach.
     *
     * @param id the enrollment id
     * @return the confirmed enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     * @throws com.student_manager.shared.exception.ValidationException if the enrollment is already confirmed or has been cancelled
     */
    @PatchMapping("{id}/confirm")
    @PreAuthorize("@ownershipGuard.canManageEnrollment(#id, authentication)")
    public ResponseEntity<EnrollmentDTO> confirm(@PathVariable Long id) {
        // log.info("PATCH /api/enrollments/{}/confirm", id);
        return ResponseEntity.ok(service.confirm(id));
    }

    /**
     * Cancels (withdraws) an enrollment, clearing any grade it carried. Access
     * is restricted by {@code @ownershipGuard}: ADMIN may cancel any
     * enrollment, but a TEACHER may only cancel enrollments in courses they teach.
     *
     * @param id the enrollment id
     * @return the cancelled enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     * @throws com.student_manager.shared.exception.ValidationException if the enrollment is already cancelled
     */
    @PatchMapping("{id}/cancel")
    @PreAuthorize("@ownershipGuard.canManageEnrollment(#id, authentication)")
    public ResponseEntity<EnrollmentDTO> cancel(@PathVariable Long id) {
        // log.info("PATCH /api/enrollments/{}/cancel", id);
        return ResponseEntity.ok(service.cancel(id));
    }

    /**
     * Assigns a grade to a confirmed enrollment. Access is restricted by
     * {@code @ownershipGuard}: ADMIN may grade any enrollment, but a TEACHER
     * may only grade enrollments in courses they teach.
     *
     * @param id the enrollment id
     * @param request the grade to assign
     * @return the updated enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     * @throws com.student_manager.shared.exception.ValidationException if the enrollment is not confirmed
     */
    @PatchMapping("{id}/grade")
    @PreAuthorize("@ownershipGuard.canManageEnrollment(#id, authentication)")
    public ResponseEntity<EnrollmentDTO> updateGrade(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGradeRequest request) {
        // log.info("PATCH /api/enrollments/{}/grade", id);
        return ResponseEntity.ok(service.updateGrade(id, request));
    }

    /**
     * Deletes an enrollment outright.
     *
     * @param id the enrollment id
     * @return HTTP 204 with no body
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     */
    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // log.info("DELETE /api/enrollments/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.student_manager.feature.enrollment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService service;

    @GetMapping
    public ResponseEntity<List<EnrollmentDTO>> getAll() {
        log.info("GET /api/enrollments");
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<EnrollmentDTO> getById(@PathVariable Long id) {
        log.info("GET /api/enrollments/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("student/{studentId}")
    public ResponseEntity<List<EnrollmentDTO>> getByStudent(@PathVariable Long studentId) {
        log.info("GET /api/enrollments/student/{}", studentId);
        return ResponseEntity.ok(service.findByStudentId(studentId));
    }

    @GetMapping("course/{courseId}")
    public ResponseEntity<List<EnrollmentDTO>> getByCourse(@PathVariable Long courseId) {
        log.info("GET /api/enrollments/course/{}", courseId);
        return ResponseEntity.ok(service.findByCourseId(courseId));
    }

    @PostMapping
    public ResponseEntity<EnrollmentDTO> create(
            @Valid @RequestBody CreateEnrollmentRequest request) {
        log.info("POST /api/enrollments");
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PatchMapping("{id}/confirm")
    public ResponseEntity<EnrollmentDTO> confirm(@PathVariable Long id) {
        log.info("PATCH /api/enrollments/{}/confirm", id);
        return ResponseEntity.ok(service.confirm(id));
    }

    @PatchMapping("{id}/cancel")
    public ResponseEntity<EnrollmentDTO> cancel(@PathVariable Long id) {
        log.info("PATCH /api/enrollments/{}/cancel", id);
        return ResponseEntity.ok(service.cancel(id));
    }

    @PatchMapping("{id}/grade")
    public ResponseEntity<EnrollmentDTO> updateGrade(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGradeRequest request) {
        log.info("PATCH /api/enrollments/{}/grade", id);
        return ResponseEntity.ok(service.updateGrade(id, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/enrollments/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

package com.student_manager.feature.course;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService service;

    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAll() {
        log.info("GET /api/courses");
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<CourseDTO> getById(@PathVariable Long id) {
        log.info("GET /api/courses/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("status/{status}")
    public ResponseEntity<List<CourseDTO>> getByStatus(@PathVariable CourseStatus status) {
        log.info("GET /api/courses/status/{}", status);
        return ResponseEntity.ok(service.findByStatus(status));
    }

    @PostMapping
    public ResponseEntity<CourseDTO> create(@Valid @RequestBody CreateCourseRequest request) {
        log.info("POST /api/courses");
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("{id}")
    public ResponseEntity<CourseDTO> update(@PathVariable Long id,
                                             @Valid @RequestBody CreateCourseRequest request) {
        log.info("PUT /api/courses/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/courses/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

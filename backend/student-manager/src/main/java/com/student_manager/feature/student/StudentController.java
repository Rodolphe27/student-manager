package com.student_manager.feature.student;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.invite.ProfileType;
import com.student_manager.feature.invite.RegistrationInviteDTO;
import com.student_manager.feature.invite.RegistrationInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Per-method entry logging removed — RequestLoggingFilter (shared/config) now
// logs method + path + status + duration for every request. The lines below
// are commented out, not deleted, for reference.
@Slf4j
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;
    private final RegistrationInviteService registrationInviteService;

    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAll() {
        // log.info("GET /api/students");
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("me")
    public ResponseEntity<StudentDTO> getMe(Authentication authentication) {
        // log.info("GET /api/students/me ({})", authentication.getName());
        return ResponseEntity.ok(service.findByAccountUsername(authentication.getName()));
    }

    @GetMapping("{id}")
    public ResponseEntity<StudentDTO> getById(@PathVariable Long id) {
        // log.info("GET /api/students/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<StudentDTO> create(@Valid @RequestBody CreateStudentRequest request) {
        // log.info("POST /api/students");
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("{id}")
    public ResponseEntity<StudentDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody CreateStudentRequest request) {
        // log.info("PUT /api/students/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // log.info("DELETE /api/students/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{id}/invite")
    public ResponseEntity<RegistrationInviteDTO> issueInvite(@PathVariable Long id, Authentication authentication) {
        // log.info("POST /api/students/{}/invite", id);
        RegistrationInviteDTO invite = registrationInviteService.issueInvite(
                Role.STUDENT, ProfileType.STUDENT, id, authentication.getName());
        return ResponseEntity.status(201).body(invite);
    }
}

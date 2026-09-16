package com.student_manager.feature.teacher;

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

@Slf4j
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService service;
    private final RegistrationInviteService registrationInviteService;

    @GetMapping
    public ResponseEntity<List<TeacherDTO>> getAll() {
        log.info("GET /api/teachers");
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("{id}")
    public ResponseEntity<TeacherDTO> getById(@PathVariable Long id) {
        log.info("GET /api/teachers/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<TeacherDTO> create(@Valid @RequestBody CreateTeacherRequest request) {
        log.info("POST /api/teachers");
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("{id}")
    public ResponseEntity<TeacherDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody CreateTeacherRequest request) {
        log.info("PUT /api/teachers/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/teachers/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{id}/invite")
    public ResponseEntity<RegistrationInviteDTO> issueInvite(@PathVariable Long id, Authentication authentication) {
        log.info("POST /api/teachers/{}/invite", id);
        RegistrationInviteDTO invite = registrationInviteService.issueInvite(
                Role.TEACHER, ProfileType.TEACHER, id, authentication.getName());
        return ResponseEntity.status(201).body(invite);
    }
}

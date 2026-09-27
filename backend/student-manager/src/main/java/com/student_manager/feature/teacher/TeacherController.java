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

// Per-method entry logging removed — RequestLoggingFilter (shared/config) now
// logs method + path + status + duration for every request. The lines below
// are commented out, not deleted, for reference.
/**
 * REST controller exposing CRUD endpoints for {@link Teacher} resources, plus
 * an endpoint to issue registration invites, under {@code /api/teachers}. Per
 * the security configuration, {@code GET} requests require the {@code TEACHER}
 * or {@code ADMIN} role, while all other methods require the {@code ADMIN} role.
 */
@Slf4j
@RestController
// TODO(SEC-12) [LOW]: no API versioning — see AuthController.
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService service;
    private final RegistrationInviteService registrationInviteService;

    /**
     * Lists every teacher in the system.
     *
     * @return 200 OK with the list of all teachers
     */
    @GetMapping
    public ResponseEntity<List<TeacherDTO>> getAll() {
        // log.info("GET /api/teachers");
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * Retrieves a single teacher by id.
     *
     * @param id the teacher id
     * @return 200 OK with the matching teacher
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no teacher exists with the given id
     */
    @GetMapping("{id}")
    public ResponseEntity<TeacherDTO> getById(@PathVariable Long id) {
        // log.info("GET /api/teachers/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Creates a new teacher profile.
     *
     * @param request the validated teacher data to create
     * @return 201 Created with the newly created teacher
     * @throws com.student_manager.shared.exception.ValidationException if the email is already in use
     */
    @PostMapping
    public ResponseEntity<TeacherDTO> create(@Valid @RequestBody CreateTeacherRequest request) {
        // log.info("POST /api/teachers");
        return ResponseEntity.status(201).body(service.create(request));
    }

    /**
     * Updates an existing teacher profile.
     *
     * @param id the id of the teacher to update
     * @param request the validated replacement teacher data
     * @return 200 OK with the updated teacher
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no teacher exists with the given id
     * @throws com.student_manager.shared.exception.ValidationException if the new email is already used by another teacher
     */
    @PutMapping("{id}")
    public ResponseEntity<TeacherDTO> update(@PathVariable Long id,
                                              @Valid @RequestBody CreateTeacherRequest request) {
        // log.info("PUT /api/teachers/{}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    /**
     * Deletes a teacher profile.
     *
     * @param id the id of the teacher to delete
     * @return 204 No Content on success
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no teacher exists with the given id
     */
    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // log.info("DELETE /api/teachers/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Issues a registration invite that lets a new user account claim the given
     * teacher profile, tying the invite to the authenticated caller as issuer.
     *
     * @param id the id of the teacher profile to invite a registrant for
     * @param authentication the authenticated caller, used as the invite's issuer
     * @return 201 Created with the newly issued invite
     */
    @PostMapping("{id}/invite")
    public ResponseEntity<RegistrationInviteDTO> issueInvite(@PathVariable Long id, Authentication authentication) {
        // log.info("POST /api/teachers/{}/invite", id);
        RegistrationInviteDTO invite = registrationInviteService.issueInvite(
                Role.TEACHER, ProfileType.TEACHER, id, authentication.getName());
        return ResponseEntity.status(201).body(invite);
    }
}

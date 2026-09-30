package com.student_manager.feature.profile;

import com.student_manager.feature.auth.AuthResponse;
import com.student_manager.feature.auth.SessionLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service endpoints under {@code /api/profile}, open to every
 * authenticated role (ADMIN, TEACHER, STUDENT). They only ever act on the
 * caller's own account, taken from the session, never from the request.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;
    private final SessionLogin sessionLogin;

    /**
     * @param authentication the session's authenticated principal
     * @return the caller's account and profile details
     */
    @GetMapping
    public ResponseEntity<ProfileDTO> get(Authentication authentication) {
        return ResponseEntity.ok(service.get(authentication.getName()));
    }

    /**
     * Updates the caller's account and profile details. The session principal
     * is the username, so a rename re-issues the session under the new name;
     * otherwise the next request would no longer find the account.
     *
     * @param request      the new values, validated before this method runs
     * @param authentication the session's authenticated principal
     * @return the updated profile
     */
    @PutMapping
    public ResponseEntity<ProfileDTO> update(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        ProfileDTO updated = service.update(authentication.getName(), request);
        if (!updated.getUsername().equals(authentication.getName())) {
            sessionLogin.start(
                    new AuthResponse(updated.getUsername(), updated.getEmail(), updated.getRole()),
                    httpRequest, httpResponse);
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * @param request        the current and new password, validated before this method runs
     * @param authentication the session's authenticated principal
     * @return 204 No Content
     */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        service.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}

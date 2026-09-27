package com.student_manager.feature.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for account registration and login. Both endpoints are
 * public (see {@code SecurityConfig}, {@code /api/auth/**} is permitted
 * without authentication) and hand a signed JWT back on success.
 */
@Slf4j
// TODO(SEC-12) [LOW]: no API versioning (here and in every other controller) — introduce
// /api/v1/... now while the surface is small, before breaking changes force a harder migration.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new account. Self-registration is always assigned the
     * STUDENT role unless the request carries a valid invite code, in which
     * case the invite determines the final role and profile link.
     *
     * @param request the registration payload (username, email, password and
     *                optional registration code), validated before this method runs
     * @return 201 Created with the new account's {@link AuthResponse}, including a JWT
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    /**
     * Authenticates an existing account and issues a new JWT.
     *
     * @param request the login credentials (username and password), validated before this method runs
     * @return 200 OK with the authenticated account's {@link AuthResponse}, including a JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

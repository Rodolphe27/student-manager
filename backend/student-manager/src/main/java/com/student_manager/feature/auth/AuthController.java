package com.student_manager.feature.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for account creation, login, and "who am I". Login is
 * public (see {@code SecurityConfig}) and starts a server-side
 * session on success, identified by the HttpOnly {@code SESSION} cookie.
 * Logout ({@code POST /api/auth/logout}) is handled by Spring Security itself.
 */
@Slf4j
// TODO(SEC-12) [LOW]: no API versioning (here and in every other controller) — introduce
// /api/v1/... now while the surface is small, before breaking changes force a harder migration.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionLogin sessionLogin;

    /**
     * Creates a new account with the requested role. ADMIN-only (see
     * {@code SecurityConfig}); the caller's own session is left untouched.
     *
     * @param request the account payload (username, email, password and role),
     *                validated before this method runs
     * @return 201 Created with the new account's {@link AuthResponse}
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    /**
     * Authenticates an existing account and starts a new session.
     *
     * @param request      the login credentials (username and password), validated before this method runs
     * @param httpRequest  the current HTTP request (the session is attached to it)
     * @param httpResponse the current HTTP response (receives the session cookie)
     * @return 200 OK with the authenticated account's {@link AuthResponse}
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponse account = authService.login(request);
        sessionLogin.start(account, httpRequest, httpResponse);
        return ResponseEntity.ok(account);
    }

    /**
     * Returns the account behind the current session. The frontend calls this
     * on startup to restore a login; without a valid session it gets 401.
     *
     * @param authentication the session's authenticated principal
     * @return 200 OK with the current account's {@link AuthResponse}
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentAccount(authentication.getName()));
    }
}

package com.student_manager.feature.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Per-method entry logging removed — RequestLoggingFilter (shared/config) now
// logs method + path + status + duration for every request. The lines below
// are commented out, not deleted, for reference.
@Slf4j
// TODO(SEC-12) [LOW]: no API versioning (here and in every other controller) — introduce
// /api/v1/... now while the surface is small, before breaking changes force a harder migration.
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // TODO(SEC-3) [HIGH]: no rate limiting on registration — allows automated account
    // creation / invite-code brute-forcing. Add a per-IP rate limiter (e.g. bucket4j).
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        // log.info("POST /api/auth/register");
        return ResponseEntity.status(201).body(authService.register(request));
    }

    // TODO(SEC-2) [HIGH]: no rate limiting / lockout here — unlimited login attempts enable
    // credential stuffing despite the anti-enumeration handling in AuthServiceImpl. Add a
    // per-IP+username rate limiter (e.g. bucket4j) in front of this endpoint.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        // log.info("POST /api/auth/login");
        return ResponseEntity.ok(authService.login(request));
    }
}

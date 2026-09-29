package com.student_manager.feature.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account summary returned by register, login, and {@code GET /api/auth/me}.
 * Carries no credential — the login itself lives in the HttpOnly session cookie.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String username;
    private String email;
    private Role role;
}

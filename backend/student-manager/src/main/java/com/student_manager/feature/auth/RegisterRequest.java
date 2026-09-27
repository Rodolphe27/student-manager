package com.student_manager.feature.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for {@code POST /api/auth/register}: username, email,
 * password, and an optional registration invite code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    // TODO: add @Pattern regex to restrict username to a safe charset (e.g. alphanumeric/._-)
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    // TODO: add @Pattern regex to enforce password strength (min length + letter/digit/symbol mix)
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Optional code from an ADMIN-issued RegistrationInvite. When present,
     * it — not this request — determines the account's role and links it to
     * the invite's target Student/Teacher profile. Absent means a plain
     * self-registration, always STUDENT with no profile link.
     */
    // TODO: add @Pattern regex to reject malformed codes early (generated codes are [A-Za-z0-9_-]{32})
    private String registrationCode;
}

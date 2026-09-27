package com.student_manager.feature.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,32}$",
            message = "Username must be 3-32 characters: letters, digits, '.', '_' or '-'")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters and include a letter and a digit")
    private String password;

    /**
     * Optional code from an ADMIN-issued RegistrationInvite. When present,
     * it — not this request — determines the account's role and links it to
     * the invite's target Student/Teacher profile. Absent means a plain
     * self-registration, always STUDENT with no profile link.
     */
    // @Pattern only validates non-null values, so this stays optional (Jakarta Bean Validation
    // spec: a @Pattern-annotated field that is null is always considered valid).
    @Pattern(regexp = "^[A-Za-z0-9_-]{32}$", message = "Registration code is malformed")
    private String registrationCode;
}

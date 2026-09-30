package com.student_manager.feature.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for creating or updating a student
 * (used by {@code POST /api/students} and {@code PUT /api/students/{id}}).
 */
@Data
@NoArgsConstructor
public class CreateStudentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Matriculation number is required")
    @Pattern(regexp = "^[A-Za-z0-9-]{2,40}$",
            message = "Matriculation number must be 2-40 characters: letters, digits or '-'")
    private String matriculationNumber;

    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    /**
     * Optional, on create only: also create a login account for this student (role
     * STUDENT) using the profile's email. Ignored by update.
     */
    private boolean createAccount;

    // Optional login name for the new account; blank = the email's local part.
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,32}$",
            message = "Username must be 3-32 characters: letters, digits, '.', '_' or '-'")
    private String accountUsername;

    // Optional initial password; blank = the configured default password.
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters and include a letter and a digit")
    private String accountPassword;

    // Version of the record the client edited (from its DTO). Optional: when present and
    // outdated, the update is rejected with 409 instead of overwriting a newer change.
    private Long version;
}

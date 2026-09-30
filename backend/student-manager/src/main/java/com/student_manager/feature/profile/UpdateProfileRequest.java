package com.student_manager.feature.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for {@code PUT /api/profile}. Account fields are always
 * required; the profile fields apply only when the account has a Student or
 * Teacher profile (first and last name are then required, checked in the service).
 */
@Data
@NoArgsConstructor
public class UpdateProfileRequest {
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,32}$",
            message = "Username must be 3-32 characters: letters, digits, '.', '_' or '-'")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    private String firstName;
    private String lastName;
    private LocalDate birthDate;

    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$",
            message = "Department must be 2-100 characters: letters, spaces, '.', ''' or '-'")
    private String department;
}

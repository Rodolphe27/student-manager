package com.student_manager.feature.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validated request payload for creating or updating a {@link Teacher} via the API.
 */
@Data
@NoArgsConstructor
public class CreateTeacherRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    // Optional field — @Pattern only validates non-null values, so a caller that omits
    // department entirely is still valid.
    @Pattern(regexp = "^[A-Za-z .'-]{2,100}$",
            message = "Department must be 2-100 characters: letters, spaces, '.', ''' or '-'")
    private String department;
}

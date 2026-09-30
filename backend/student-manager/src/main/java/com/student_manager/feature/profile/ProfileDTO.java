package com.student_manager.feature.profile;

import com.student_manager.feature.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * The caller's own account plus, when one is linked, their Student or Teacher
 * profile details. Profile fields are {@code null} for accounts without a
 * profile (e.g. ADMIN) and for fields the profile type does not have.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private String username;
    private String email;
    private Role role;
    private String firstName;
    private String lastName;
    /** Student only; read-only, only an ADMIN can change it via the student roster. */
    private String matriculationNumber;
    /** Student only. */
    private LocalDate birthDate;
    /** Teacher only. */
    private String department;
}

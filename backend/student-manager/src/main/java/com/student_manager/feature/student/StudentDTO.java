package com.student_manager.feature.student;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Client-facing view of a {@link Student}, including the derived
 * {@link #fullName} convenience field.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String matriculationNumber;
    private LocalDate birthDate;
    private String email;
    private String fullName;
}

package com.student_manager.feature.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class CreateStudentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Matriculation number is required")
    private String matriculationNumber;

    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;
}

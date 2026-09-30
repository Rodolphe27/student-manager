package com.student_manager.feature.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Request payload for {@code POST /api/terms}. */
@Data
@NoArgsConstructor
public class CreateTermRequest {

    @NotBlank(message = "Term name is required")
    @Size(max = 100, message = "Term name must be at most 100 characters")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;
}

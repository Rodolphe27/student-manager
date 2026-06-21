package com.student_manager.feature.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateGradeRequest {

    @NotNull(message = "Grade is required")
    private Grade grade;
}

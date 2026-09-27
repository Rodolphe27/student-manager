package com.student_manager.feature.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for assigning a grade to a confirmed enrollment
 * (used by {@code PATCH /api/enrollments/{id}/grade}).
 */
@Data
@NoArgsConstructor
public class UpdateGradeRequest {

    @NotNull(message = "Grade is required")
    private Grade grade;
}

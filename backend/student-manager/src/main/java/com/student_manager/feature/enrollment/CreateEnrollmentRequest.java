package com.student_manager.feature.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for enrolling a student in a course
 * (used by {@code POST /api/enrollments}).
 */
@Data
@NoArgsConstructor
public class CreateEnrollmentRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Course ID is required")
    private Long courseId;
}

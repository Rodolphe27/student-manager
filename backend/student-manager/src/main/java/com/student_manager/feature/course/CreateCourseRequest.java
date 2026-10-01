package com.student_manager.feature.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validated request payload for creating or updating a {@link Course} via the API.
 */
@Data
@NoArgsConstructor
public class CreateCourseRequest {

    @NotBlank(message = "Course code is required")
    @Pattern(regexp = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$",
            message = "Course code must be alphanumeric segments separated by '-' (e.g. CS-101)")
    private String code;

    @NotBlank(message = "Course title is required")
    private String title;

    private String description;

    @Min(value = 1, message = "Credit hours must be at least 1")
    @Max(value = 10, message = "Credit hours must be at most 10")
    private int creditHours;

    private CourseStatus status = CourseStatus.ACTIVE;

    // Optional: the teacher who runs the course (null = unassigned). Only the assigned
    // teacher (and ADMINs) may manage the course's enrollments.
    private Long teacherId;

    // Optional: the term the course belongs to (null = none).
    private Long termId;

    // Version of the record the client edited (from its DTO). Optional: when present and
    // outdated, the update is rejected with 409 instead of overwriting a newer change.
    private Long version;
}

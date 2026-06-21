package com.student_manager.feature.course;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateCourseRequest {

    @NotBlank(message = "Course code is required")
    private String code;

    @NotBlank(message = "Course title is required")
    private String title;

    private String description;

    @Min(value = 1, message = "Credit hours must be at least 1")
    @Max(value = 10, message = "Credit hours must be at most 10")
    private int creditHours;

    private CourseStatus status = CourseStatus.ACTIVE;
}

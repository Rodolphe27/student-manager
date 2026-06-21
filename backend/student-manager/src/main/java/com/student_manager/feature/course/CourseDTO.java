package com.student_manager.feature.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String code;
    private String title;
    private String description;
    private int creditHours;
    private CourseStatus status;
    private boolean active;
}

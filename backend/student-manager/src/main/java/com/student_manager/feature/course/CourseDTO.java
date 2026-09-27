package com.student_manager.feature.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object exposing a {@link Course} to API clients, decoupling
 * the wire representation from the JPA entity.
 */
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

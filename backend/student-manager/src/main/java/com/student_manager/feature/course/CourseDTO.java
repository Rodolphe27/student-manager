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
    // Nullable: a course may have no assigned teacher / term yet.
    private Long teacherId;
    private String teacherName;
    private Long termId;
    private String termName;
    // Optimistic-lock version; send it back on update so a stale edit is rejected (409).
    private long version;
}

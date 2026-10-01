package com.student_manager.feature.enrollment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Flattened, client-facing view of an {@link Enrollment}, denormalizing the
 * related student's name and the course's title/code so callers don't need
 * separate lookups.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseTitle;
    private String courseCode;
    private LocalDate enrolledAt;
    private EnrollmentStatus status;
    private Grade grade;
    private boolean confirmed;
    // false = the student has a grade they have not acknowledged yet ("new grade").
    private boolean gradeSeen;
}

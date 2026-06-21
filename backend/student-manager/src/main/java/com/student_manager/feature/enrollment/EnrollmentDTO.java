package com.student_manager.feature.enrollment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}

package com.student_manager.feature.enrollment;

/**
 * Lifecycle states of an {@link Enrollment}: created as {@link #PENDING},
 * then either {@link #CONFIRMED} (eligible to be graded) or
 * {@link #CANCELLED} (withdrawn; carries no grade).
 */
public enum EnrollmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}

package com.student_manager.feature.enrollment;

/**
 * The letter grade assigned to a confirmed {@link Enrollment}, or
 * {@link #NOT_GRADED} when none has been assigned yet (the default, and the
 * value forced back on when an enrollment is cancelled).
 */
public enum Grade {
    A,
    B,
    C,
    D,
    F,
    NOT_GRADED
}

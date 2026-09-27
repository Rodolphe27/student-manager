package com.student_manager.feature.auth;

/**
 * The set of account roles used for authorization throughout the app. Each
 * name maps directly to a Spring Security {@code ROLE_*} authority (e.g.
 * {@code ROLE_ADMIN}).
 */
public enum Role {
    STUDENT,
    TEACHER,
    ADMIN
}

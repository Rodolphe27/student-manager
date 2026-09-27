package com.student_manager.feature.invite;

/**
 * The kind of academic profile a {@link RegistrationInvite} grants access to.
 * Used to select the matching {@link ProfileResolver} implementation.
 */
public enum ProfileType {
    STUDENT,
    TEACHER
}

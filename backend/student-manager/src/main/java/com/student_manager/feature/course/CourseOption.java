package com.student_manager.feature.course;

/**
 * Minimal course view for selection lists (e.g. the enrollment form's course
 * dropdown; status lets the form offer only ACTIVE courses) — an interface-based
 * projection returned by
 * {@link CourseRepository#findAllProjectedBy}.
 */
public interface CourseOption {

    Long getId();

    String getCode();

    String getTitle();

    CourseStatus getStatus();
}

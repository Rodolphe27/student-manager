package com.student_manager.feature.teacher;

/** Lightweight teacher projection for selection lists (e.g. the course form). */
public record TeacherOption(Long id, String fullName, String department) {
}

package com.student_manager.feature.student;

/**
 * Minimal student view for selection lists (e.g. the enrollment form's
 * student dropdown) — a DTO projection filled directly by
 * {@link StudentRepository#findAllOptions()}.
 *
 * @param id                  the student id
 * @param fullName            "first last"
 * @param matriculationNumber the matriculation number
 */
public record StudentOption(Long id, String fullName, String matriculationNumber) {
}

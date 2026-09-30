package com.student_manager.feature.course;

import java.time.LocalDate;

/** Client-facing view of a {@link Term}. */
public record TermDTO(Long id, String name, LocalDate startDate, LocalDate endDate) {

    static TermDTO of(Term term) {
        return new TermDTO(term.getId(), term.getName(), term.getStartDate(), term.getEndDate());
    }
}

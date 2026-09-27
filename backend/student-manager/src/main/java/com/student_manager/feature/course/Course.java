package com.student_manager.feature.course;

import com.student_manager.feature.teacher.Teacher;
import com.student_manager.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a course offered by the institution. A course has a
 * unique code, belongs to an optional {@link Term} and an optional
 * {@link Teacher}, and carries a {@link CourseStatus} lifecycle state.
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private int creditHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.ACTIVE;

    // Nullable, unlike the proposed design's NOT NULL term_id: this app has no
    // migration tool (ddl-auto=update) and existing production courses have no
    // term to backfill. Tighten to non-null once Flyway lands and a backfill
    // migration can run first.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    /**
     * Checks whether this course's status is {@link CourseStatus#ACTIVE}.
     *
     * @return {@code true} if the course status is {@code ACTIVE}, {@code false} otherwise
     */
    public boolean isActive() {
        return CourseStatus.ACTIVE.equals(this.status);
    }
}

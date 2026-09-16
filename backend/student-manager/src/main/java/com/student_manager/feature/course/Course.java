package com.student_manager.feature.course;

import com.student_manager.feature.teacher.Teacher;
import com.student_manager.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public boolean isActive() {
        return CourseStatus.ACTIVE.equals(this.status);
    }
}

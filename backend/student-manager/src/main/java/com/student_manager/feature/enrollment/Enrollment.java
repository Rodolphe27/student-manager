package com.student_manager.feature.enrollment;

import com.student_manager.feature.course.Course;
import com.student_manager.feature.student.Student;
import com.student_manager.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Links a {@link Student} to a {@link Course} they are (or were) taking,
 * tracking the enrollment's {@link EnrollmentStatus lifecycle status} and,
 * once confirmed, its {@link Grade}.
 * <p>
 * The table carries a DB-level unique constraint on
 * ({@code student_id}, {@code course_id}): a student can have at most one
 * enrollment row per course, ever — re-enrolling in the same course is not
 * supported by creating a second row. This means a {@link EnrollmentStatus#CANCELLED}
 * enrollment cannot simply be re-created to "re-enroll" the student; the
 * existing row would have to be transitioned back instead (see
 * {@code EnrollmentServiceImpl}'s handling of cancellation, and the
 * "issue #33" comment there, for how cancellation is kept consistent with
 * this constraint).
 */
@Entity
@Table(name = "enrollments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDate enrolledAt = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade = Grade.NOT_GRADED;

    /**
     * Whether this enrollment has been confirmed.
     *
     * @return {@code true} if {@link #getStatus()} is {@link EnrollmentStatus#CONFIRMED}
     */
    public boolean isConfirmed() {
        return EnrollmentStatus.CONFIRMED.equals(this.status);
    }
}

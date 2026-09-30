package com.student_manager.feature.student;

import com.student_manager.feature.auth.User;
import com.student_manager.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A student's academic profile: personal details, matriculation number, and
 * (once registration is claimed) the linked login {@link #account}.
 */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseEntity {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String matriculationNumber;

    private LocalDate birthDate;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The account linked to this profile, set when an admin links an account
     * targeting this student is claimed. Nullable: a profile can exist (created
     * by an ADMIN) before anyone has registered against it.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User account;

    /**
     * The student's display name.
     *
     * @return {@link #getFirstName()} and {@link #getLastName()} joined by a space
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

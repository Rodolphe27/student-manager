package com.student_manager.feature.teacher;

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

/**
 * JPA entity representing a teacher profile. A teacher profile may exist
 * before any user account is linked to it (e.g. created by an ADMIN ahead of
 * an account being created), so {@link #account} is nullable.
 */
@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Teacher extends BaseEntity {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String department;

    /**
     * The login account of this teacher, set when an ADMIN creates the profile together
     * with an account, or when the teacher saves their profile page. Nullable: a profile
     * can exist without a login.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User account;

    /**
     * Builds the teacher's display name from their first and last name.
     *
     * @return the concatenation of first name and last name, separated by a space
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

package com.student_manager.feature.auth;

import com.student_manager.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A login account: credentials, role, and active/inactive status. Distinct
 * from {@code Student}/{@code Teacher}, which hold the person's profile data
 * and are matched to a {@code User} by email or a direct association.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Whether this account is enabled and may authenticate. Declared
     * explicitly (rather than relying on Lombok's boolean-getter naming) so
     * the accessor is unambiguously named {@code isActive()}.
     *
     * @return {@code true} if the account is active
     */
    public boolean isActive() {
        return active;
    }
}

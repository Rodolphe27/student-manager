package com.student_manager.feature.invite;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * A single-use, time-limited token that lets a new user account claim
 * (link itself to) an existing Student or Teacher profile and be assigned
 * the corresponding {@link Role}.
 */
@Entity
@Table(name = "registration_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationInvite extends BaseEntity {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ProfileType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Factory method instead of a public setter-driven build: guarantees the
     * code is high-entropy (never {@code Math.random()} — this token grants
     * account access to a specific profile) and the expiry is always set.
     *
     * @param role the role to grant the account that claims this invite
     * @param targetType the kind of profile (Student or Teacher) this invite targets
     * @param targetId the id of the target profile
     * @param issuedBy the user issuing the invite, may be {@code null}
     * @param ttl how long the invite remains valid, added to the current time
     * @return a new, unused invite with a freshly generated code and expiry
     */
    public static RegistrationInvite issue(Role role, ProfileType targetType, Long targetId,
                                            User issuedBy, Duration ttl) {
        RegistrationInvite invite = new RegistrationInvite();
        invite.code = generateCode();
        invite.role = role;
        invite.targetType = targetType;
        invite.targetId = targetId;
        invite.issuedBy = issuedBy;
        invite.expiresAt = LocalDateTime.now().plus(ttl);
        return invite;
    }

    private static String generateCode() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Checks whether this invite's expiry time has passed.
     *
     * @return {@code true} if the current time is after {@code expiresAt}
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks whether this invite has already been claimed.
     *
     * @return {@code true} if the invite has a recorded usage timestamp
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Marks this invite as used by recording the current time.
     */
    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}

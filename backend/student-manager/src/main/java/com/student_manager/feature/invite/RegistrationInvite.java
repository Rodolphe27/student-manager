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

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}

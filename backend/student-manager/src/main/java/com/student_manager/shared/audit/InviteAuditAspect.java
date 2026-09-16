package com.student_manager.shared.audit;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.invite.ProfileType;
import com.student_manager.feature.invite.RegistrationInviteDTO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting audit trail for registration-invite issuance and claims — a
 * security-sensitive operation (it grants account access to a specific
 * student/teacher profile) kept out of {@code RegistrationInviteServiceImpl}'s
 * business logic so the log line can't be forgotten or skipped by a future
 * code path. Deliberately never logs the raw invite code.
 */
@Slf4j
@Aspect
@Component
public class InviteAuditAspect {

    @AfterReturning(
            pointcut = "execution(* com.student_manager.feature.invite.RegistrationInviteServiceImpl.issueInvite(..)) "
                    + "&& args(role, targetType, targetId, issuerUsername)",
            returning = "invite",
            argNames = "role,targetType,targetId,issuerUsername,invite")
    public void afterIssue(Role role, ProfileType targetType, Long targetId, String issuerUsername,
                            RegistrationInviteDTO invite) {
        log.info("AUDIT invite issued: issuer={} role={} targetType={} targetId={} expiresAt={}",
                issuerUsername, role, targetType, targetId, invite.getExpiresAt());
    }

    @AfterReturning(
            pointcut = "execution(* com.student_manager.feature.invite.RegistrationInviteServiceImpl.claim(..)) "
                    + "&& args(code, user)",
            returning = "savedUser",
            argNames = "code,user,savedUser")
    public void afterClaim(String code, User user, User savedUser) {
        log.info("AUDIT invite claimed: account={} userId={} role={}",
                savedUser.getUsername(), savedUser.getId(), savedUser.getRole());
    }
}

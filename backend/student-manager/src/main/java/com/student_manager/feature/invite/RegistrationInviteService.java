package com.student_manager.feature.invite;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;

/**
 * Manages the lifecycle of {@link RegistrationInvite}s: issuing them for an
 * existing academic profile, and claiming them to create a linked user
 * account.
 */
public interface RegistrationInviteService {

    /**
     * Issues a single-use invite linking a future account to an existing
     * {@code Student}/{@code Teacher} profile. Only an ADMIN may call this
     * (enforced at the controller); the role is fixed by the caller, never
     * taken from client input at claim time.
     *
     * @param role the role to grant the account that eventually claims this invite
     * @param targetType the kind of profile (Student or Teacher) targeted
     * @param targetId the id of the target profile
     * @param issuerUsername the username of the issuing user, used to record who issued it
     * @return the created invite as a DTO
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if the target profile does not exist
     * @throws com.student_manager.shared.exception.ValidationException if an active (unused, unexpired) invite already exists for the target
     */
    RegistrationInviteDTO issueInvite(Role role, ProfileType targetType, Long targetId, String issuerUsername);

    /**
     * Validates {@code code}, assigns its role to {@code user}, persists the
     * user, links the target profile to it, and marks the invite used — all in
     * one transaction. Returns the saved user.
     *
     * @param code the invite code to claim
     * @param user the not-yet-persisted user account claiming the invite
     * @return the saved, role-assigned user
     * @throws com.student_manager.shared.exception.InvalidInviteException if the code is unknown, already used, or expired
     */
    User claim(String code, User user);
}

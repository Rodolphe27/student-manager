package com.student_manager.feature.invite;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;

public interface RegistrationInviteService {

    /**
     * Issues a single-use invite linking a future account to an existing
     * {@code Student}/{@code Teacher} profile. Only an ADMIN may call this
     * (enforced at the controller); the role is fixed by the caller, never
     * taken from client input at claim time.
     */
    RegistrationInviteDTO issueInvite(Role role, ProfileType targetType, Long targetId, String issuerUsername);

    /**
     * Validates {@code code}, assigns its role to {@code user}, persists the
     * user, links the target profile to it, and marks the invite used — all in
     * one transaction. Returns the saved user.
     */
    User claim(String code, User user);
}

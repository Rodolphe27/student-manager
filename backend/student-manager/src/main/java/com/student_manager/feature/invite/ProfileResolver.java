package com.student_manager.feature.invite;

import com.student_manager.feature.auth.User;

/**
 * Strategy for linking a claimed {@link RegistrationInvite} to the academic
 * profile (Student or Teacher) it targets. One implementation per
 * {@link ProfileType}, selected at runtime by {@code RegistrationInviteServiceImpl}
 * instead of an if/else on the enum — adding a new profile type means adding a
 * new {@code @Component}, not touching the invite service.
 */
public interface ProfileResolver {

    /**
     * Identifies which {@link ProfileType} this resolver handles, used to
     * pick the right implementation at runtime.
     *
     * @return the profile type supported by this resolver
     */
    ProfileType supports();

    /**
     * Checks whether the target profile (Student or Teacher, depending on
     * {@link #supports()}) still exists.
     *
     * @param targetId the id of the profile to check
     * @return {@code true} if the profile exists, {@code false} otherwise
     */
    boolean profileExists(Long targetId);

    /**
     * Links the given user account to the target profile, persisting the
     * association.
     *
     * @param targetId the id of the profile to link
     * @param user the account to attach to the profile
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no profile exists with {@code targetId}
     */
    void linkAccount(Long targetId, User user);
}

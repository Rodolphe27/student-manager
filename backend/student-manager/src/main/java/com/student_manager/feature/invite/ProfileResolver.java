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

    ProfileType supports();

    boolean profileExists(Long targetId);

    void linkAccount(Long targetId, User user);
}

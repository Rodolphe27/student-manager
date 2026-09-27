package com.student_manager.feature.invite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link RegistrationInvite} persistence and
 * lookups by invite code or target profile.
 */
public interface RegistrationInviteRepository extends JpaRepository<RegistrationInvite, Long> {

    /**
     * Finds the invite with the given code, if any.
     *
     * @param code the unique invite code
     * @return the matching invite, or empty if none exists
     */
    Optional<RegistrationInvite> findByCode(String code);

    /**
     * Finds all invites issued for a given target profile, including used and
     * expired ones.
     *
     * @param targetType the kind of profile (Student or Teacher)
     * @param targetId the id of the target profile
     * @return the invites issued for that profile
     */
    List<RegistrationInvite> findByTargetTypeAndTargetId(ProfileType targetType, Long targetId);
}

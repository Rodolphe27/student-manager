package com.student_manager.feature.invite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationInviteRepository extends JpaRepository<RegistrationInvite, Long> {
    Optional<RegistrationInvite> findByCode(String code);
    List<RegistrationInvite> findByTargetTypeAndTargetId(ProfileType targetType, Long targetId);
}

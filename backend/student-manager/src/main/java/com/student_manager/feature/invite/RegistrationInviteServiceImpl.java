package com.student_manager.feature.invite;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.auth.UserRepository;
import com.student_manager.shared.exception.InvalidInviteException;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegistrationInviteServiceImpl implements RegistrationInviteService {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final RegistrationInviteRepository repository;
    private final UserRepository userRepository;
    private final Map<ProfileType, ProfileResolver> resolvers;

    public RegistrationInviteServiceImpl(RegistrationInviteRepository repository,
                                          UserRepository userRepository,
                                          List<ProfileResolver> resolverBeans) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.resolvers = resolverBeans.stream()
                .collect(Collectors.toMap(ProfileResolver::supports, Function.identity()));
    }

    @Override
    @Transactional
    public RegistrationInviteDTO issueInvite(Role role, ProfileType targetType, Long targetId, String issuerUsername) {
        log.info("Issuing {} invite for {} {}", role, targetType, targetId);

        ProfileResolver resolver = resolverFor(targetType);
        if (!resolver.profileExists(targetId)) {
            throw new ResourceNotFoundException(targetType.name(), targetId);
        }

        boolean hasActiveInvite = repository.findByTargetTypeAndTargetId(targetType, targetId).stream()
                .anyMatch(existing -> !existing.isUsed() && !existing.isExpired());
        if (hasActiveInvite) {
            throw new ValidationException(
                    "An active invite already exists for this " + targetType.name().toLowerCase());
        }

        User issuer = userRepository.findByUsername(issuerUsername).orElse(null);
        RegistrationInvite invite = RegistrationInvite.issue(role, targetType, targetId, issuer, INVITE_TTL);
        RegistrationInvite saved = repository.save(invite);
        log.info("Invite issued with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public User claim(String code, User user) {
        log.info("Claiming invite");
        RegistrationInvite invite = repository.findByCode(code)
                .orElseThrow(() -> new InvalidInviteException("Invalid registration code"));

        if (invite.isUsed()) {
            throw new InvalidInviteException("Registration code has already been used");
        }
        if (invite.isExpired()) {
            throw new InvalidInviteException("Registration code has expired");
        }

        user.setRole(invite.getRole());
        User savedUser = userRepository.save(user);

        ProfileResolver resolver = resolverFor(invite.getTargetType());
        resolver.linkAccount(invite.getTargetId(), savedUser);

        invite.markUsed();
        repository.save(invite);

        log.info("Invite claimed by new user id: {}", savedUser.getId());
        return savedUser;
    }

    private ProfileResolver resolverFor(ProfileType targetType) {
        ProfileResolver resolver = resolvers.get(targetType);
        if (resolver == null) {
            throw new ValidationException("Unsupported profile type: " + targetType);
        }
        return resolver;
    }

    private RegistrationInviteDTO toDTO(RegistrationInvite invite) {
        return new RegistrationInviteDTO(
                invite.getId(),
                invite.getCode(),
                invite.getRole(),
                invite.getTargetType(),
                invite.getTargetId(),
                invite.getExpiresAt());
    }
}

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

/**
 * Default {@link RegistrationInviteService} implementation. Delegates
 * profile-specific existence checks and account linking to the
 * {@link ProfileResolver} registered for each {@link ProfileType}.
 */
// All log.info(...) calls below are commented out — InviteAuditAspect
// (shared/audit) already logs issuance and claim as AUDIT lines with more
// detail, from outside this class. @Slf4j is kept for any future log line.
@Slf4j
@Service
public class RegistrationInviteServiceImpl implements RegistrationInviteService {

    private static final Duration INVITE_TTL = Duration.ofDays(7);

    private final RegistrationInviteRepository repository;
    private final UserRepository userRepository;
    private final Map<ProfileType, ProfileResolver> resolvers;

    /**
     * Creates the service, indexing the injected {@link ProfileResolver}
     * beans by the {@link ProfileType} each one supports.
     *
     * @param repository invite persistence
     * @param userRepository user persistence, used to look up the issuer and save claimed accounts
     * @param resolverBeans all registered profile resolvers, one per {@link ProfileType}
     */
    public RegistrationInviteServiceImpl(RegistrationInviteRepository repository,
                                          UserRepository userRepository,
                                          List<ProfileResolver> resolverBeans) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.resolvers = resolverBeans.stream()
                .collect(Collectors.toMap(ProfileResolver::supports, Function.identity()));
    }

    /**
     * {@inheritDoc}
     *
     * @param role the role to grant the account that eventually claims this invite
     * @param targetType the kind of profile (Student or Teacher) targeted
     * @param targetId the id of the target profile
     * @param issuerUsername the username of the issuing user
     * @return the created invite as a DTO
     * @throws ResourceNotFoundException if the target profile does not exist
     * @throws ValidationException if an active (unused, unexpired) invite already exists for the target
     */
    @Override
    @Transactional
    public RegistrationInviteDTO issueInvite(Role role, ProfileType targetType, Long targetId, String issuerUsername) {
        // Commented out, not deleted: InviteAuditAspect#afterIssue already logs
        // this event (with more detail — issuer, expiresAt) after a successful
        // return, so this line was a duplicate of the same audit entry.
        // log.info("Issuing {} invite for {} {}", role, targetType, targetId);

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
        // log.info("Invite issued with id: {}", saved.getId()); // superseded by InviteAuditAspect#afterIssue
        return toDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param code the invite code to claim
     * @param user the not-yet-persisted user account claiming the invite
     * @return the saved, role-assigned user
     * @throws InvalidInviteException if the code is unknown, already used, or expired
     */
    @Override
    @Transactional
    public User claim(String code, User user) {
        // Commented out, not deleted: InviteAuditAspect#afterClaim already logs
        // this event (with account/userId/role) after a successful return.
        // log.info("Claiming invite");
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

        // log.info("Invite claimed by new user id: {}", savedUser.getId()); // superseded by InviteAuditAspect#afterClaim
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

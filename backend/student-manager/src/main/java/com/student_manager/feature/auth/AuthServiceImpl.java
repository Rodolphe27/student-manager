package com.student_manager.feature.auth;

import com.student_manager.shared.exception.InvalidCredentialsException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Unlike the CRUD services, the log.info(...) calls below are intentionally
// kept active (not commented out) as part of the logging-centralization
// cleanup. They're a security-audit trail for authentication — who
// registered/logged in and when — not a restatement of the request path, and
// each one carries data (username, generated id) that RequestLoggingFilter
// (shared/config) can't see..
/**
 * Default {@link AuthService} implementation. Handles password hashing,
 * username/email uniqueness checks, and admin-created accounts.
 * Starting the session after a successful register/login is the controller's
 * job (see {@link SessionLogin}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountProvisioner accountProvisioner;

    /**
     * Creates a new account with the requested role and {@code active = true}.
     * Only reachable by an ADMIN (see {@code SecurityConfig}); there is no
     * public self-registration.
     *
     * @param request the account payload
     * @return an account summary for the newly created user
     * @throws ValidationException if the username or email is already taken
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());

        User saved = accountProvisioner.create(
                request.getUsername(), request.getEmail(), request.getPassword(), request.getRole());
        log.info("User registered with id: {}", saved.getId());

        return toResponse(saved);
    }

    /**
     * Authenticates an account by username and password. Every failure mode
     * (unknown username, inactive account, wrong password) is reported via
     * the same exception so a caller cannot enumerate valid usernames.
     *
     * @param request the login credentials
     * @return an account summary for the authenticated user
     * @throws InvalidCredentialsException if the username is unknown, the account
     *                                      is inactive, or the password does not match
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: {}", request.getUsername());

        // Every failure below throws the same exception with the same message so a
        // caller cannot tell an unknown username from a wrong password (issue: user
        // enumeration via /api/auth/login).
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("User logged in: {}", user.getUsername());
        return toResponse(user);
    }

    /**
     * Looks up the account behind the current session. An account that was
     * deleted or deactivated after login is treated like a failed login, so
     * the stale session gets 401 and the frontend logs out.
     *
     * @param username the session principal's username
     * @return an account summary for that user
     * @throws InvalidCredentialsException if the account no longer exists or is inactive
     */
    @Override
    public AuthResponse currentAccount(String username) {
        User user = userRepository.findByUsername(username)
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(user.getUsername(), user.getEmail(), user.getRole());
    }
}

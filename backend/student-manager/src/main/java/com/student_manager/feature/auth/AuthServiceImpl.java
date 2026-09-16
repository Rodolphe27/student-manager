package com.student_manager.feature.auth;

import com.student_manager.feature.invite.RegistrationInviteService;
import com.student_manager.shared.exception.InvalidCredentialsException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationInviteService registrationInviteService;

    @Override
    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // Self-registration is always STUDENT. Any client-supplied role is ignored to
        // prevent privilege escalation (issue #43). Elevated roles must be granted via
        // a separate, ADMIN-authenticated endpoint.
        user.setRole(Role.STUDENT);
        user.setActive(true);

        User saved;
        if (request.getRegistrationCode() != null && !request.getRegistrationCode().isBlank()) {
            // The invite — not this request — decides the final role and links the
            // account to its target profile. claim() overwrites user.role below.
            saved = registrationInviteService.claim(request.getRegistrationCode(), user);
        } else {
            saved = userRepository.save(user);
        }
        log.info("User registered with id: {}", saved.getId());

        String token = jwtUtil.generateToken(saved.getUsername(), saved.getRole().name());
        return new AuthDTO.AuthResponse(token, saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    @Override
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
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

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        log.info("User logged in: {}", user.getUsername());
        return new AuthDTO.AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }
}

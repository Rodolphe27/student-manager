package com.student_manager.feature.auth;

import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
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
        user.setRole(request.getRole() != null ? request.getRole() : Role.STUDENT);
        user.setActive(true);

        User saved = userRepository.save(user);
        log.info("User registered with id: {}", saved.getId());

        String token = jwtUtil.generateToken(saved.getUsername(), saved.getRole().name());
        return new AuthDTO.AuthResponse(token, saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    @Override
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        log.info("Login attempt: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

        if (!user.isActive()) {
            throw new ValidationException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        log.info("User logged in: {}", user.getUsername());
        return new AuthDTO.AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }
}

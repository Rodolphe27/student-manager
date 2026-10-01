package com.student_manager.feature.auth;

import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.validation.ValidationPatterns;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * Creates login accounts on behalf of an ADMIN: directly via
 * {@code POST /api/auth/register}, or together with a new Student/Teacher
 * profile. Applies the two conveniences of admin-created accounts: a missing
 * password falls back to the configured default ({@code app.accounts.default-password}),
 * and a missing username is derived from the email.
 */
@Component
public class AccountProvisioner {

    private static final Pattern VALID_USERNAME = Pattern.compile(ValidationPatterns.USERNAME);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public AccountProvisioner(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.accounts.default-password}") String defaultPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    /**
     * Creates and saves an active account.
     *
     * @param username the login name, or blank to derive it from the email's local part
     * @param email    the account email
     * @param password the initial password, or blank to use the configured default
     * @param role     the role to grant
     * @return the saved account
     * @throws ValidationException if the username/email is taken or no valid username can be derived
     */
    @Transactional
    public User create(String username, String email, String password, Role role) {
        String name = isBlank(username) ? deriveUsername(email) : username.trim();
        if (userRepository.existsByUsername(name)) {
            throw new ValidationException("Username already exists: " + name);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email already exists: " + email);
        }

        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(isBlank(password) ? defaultPassword : password));
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private static String deriveUsername(String email) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String name = local.replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        if (!VALID_USERNAME.matcher(name).matches()) {
            throw new ValidationException("Could not derive a username from the email; please enter one");
        }
        return name;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

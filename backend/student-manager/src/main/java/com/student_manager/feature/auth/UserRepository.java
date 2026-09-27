package com.student_manager.feature.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for {@link User} accounts.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Looks up an account by its unique username.
     *
     * @param username the username to search for
     * @return the matching account, or empty if none exists
     */
    Optional<User> findByUsername(String username);

    /**
     * Looks up an account by its unique email address.
     *
     * @param email the email address to search for
     * @return the matching account, or empty if none exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether an account with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if an account with that username exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an account with the given email already exists.
     *
     * @param email the email address to check
     * @return {@code true} if an account with that email exists
     */
    boolean existsByEmail(String email);
}

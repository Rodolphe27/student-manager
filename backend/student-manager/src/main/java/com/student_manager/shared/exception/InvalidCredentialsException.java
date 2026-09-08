package com.student_manager.shared.exception;

/**
 * Thrown when a login attempt fails. Carries a single, non-specific message for
 * every failure mode (unknown username, wrong password, disabled account) so the
 * API never reveals which usernames exist (user-enumeration hardening).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}

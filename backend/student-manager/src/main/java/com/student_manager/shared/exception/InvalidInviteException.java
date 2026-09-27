package com.student_manager.shared.exception;

/**
 * Thrown when a registration-invite code is unknown, expired, or already
 * claimed.
 */
public class InvalidInviteException extends RuntimeException {

    /**
     * Creates the exception with a message describing why the invite is invalid.
     *
     * @param message the detail message
     */
    public InvalidInviteException(String message) {
        super(message);
    }
}

package com.student_manager.shared.exception;

/**
 * Thrown for application-level validation failures that aren't covered by
 * bean-validation annotations (e.g. a duplicate username/email), translated
 * by {@link GlobalExceptionHandler} into a 400 response.
 */
public class ValidationException extends RuntimeException {

    /**
     * Creates the exception with a message describing the validation failure.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
    }
}

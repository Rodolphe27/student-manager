package com.student_manager.shared.exception;

/**
 * Thrown when a requested entity does not exist, translated by
 * {@link GlobalExceptionHandler} into a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates the exception with a fully custom message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a standardized "{@code <resource> not found
     * with id: <id>}" message.
     *
     * @param resource the human-readable resource name, e.g. "Student"
     * @param id       the id that could not be found
     */
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}

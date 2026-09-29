package com.student_manager.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception-to-HTTP-response translation for every controller.
 * Maps each known application exception (and a handful of Spring framework
 * exceptions) to an appropriate status code and a consistent
 * {@link ErrorResponse} body.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles a missing resource lookup.
     *
     * @param ex the thrown exception, carrying the not-found message
     * @return 404 Not Found with the exception's message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), null, LocalDateTime.now()));
    }

    /**
     * Handles a failed login attempt.
     *
     * @param ex the thrown exception, carrying the generic credentials message
     * @return 401 Unauthorized with the exception's message
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Rejected login attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, ex.getMessage(), null, LocalDateTime.now()));
    }

    /**
     * Handles an application-level validation failure (e.g. duplicate
     * username/email).
     *
     * @param ex the thrown exception, carrying the validation message
     * @return 400 Bad Request with the exception's message
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage(), null, LocalDateTime.now()));
    }

    /**
     * Handles an invalid, expired, or already-claimed registration invite.
     *
     * @param ex the thrown exception, carrying the invite-specific message
     * @return 400 Bad Request with the exception's message
     */
    @ExceptionHandler(InvalidInviteException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInvite(InvalidInviteException ex) {
        log.warn("Invalid registration invite: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage(), null, LocalDateTime.now()));
    }

    /**
     * Handles bean-validation failures on a {@code @Valid} request body,
     * collecting each field's error message.
     *
     * @param ex the thrown exception, carrying the binding result
     * @return 400 Bad Request with a per-field map of validation errors as {@code details}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation errors: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Validation failed", errors, LocalDateTime.now()));
    }

    /**
     * Handles a method-level ({@code @PreAuthorize}) authorization denial.
     *
     * @param ex the thrown exception
     * @return 403 Forbidden with a generic access-denied message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Reaches here only for method-level (@PreAuthorize) denials; filter-level
        // denials are translated to 403 before the dispatcher. Without this, the
        // catch-all below would turn every such denial into a 500.
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Access denied", null, LocalDateTime.now()));
    }

    /**
     * Handles a request parameter/path variable that cannot be converted to
     * its target type (e.g. an invalid enum value).
     *
     * @param ex the thrown exception, carrying the offending parameter name
     * @return 400 Bad Request naming the invalid parameter
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // e.g. an invalid enum value in a path variable like /api/courses/status/{status}.
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        log.warn("{}: {}", message, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, message, null, LocalDateTime.now()));
    }

    /**
     * Handles a request body that cannot be parsed (e.g. malformed JSON).
     *
     * @param ex the thrown exception
     * @return 400 Bad Request with a generic malformed-body message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // Malformed/unparseable JSON request body.
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Malformed request body", null, LocalDateTime.now()));
    }

    /**
     * Handles an update based on an outdated copy of a record (optimistic
     * locking, see {@code BaseEntity#version}): someone else saved it first.
     *
     * @param ex the thrown exception
     * @return 409 Conflict asking the user to reload
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409,
                        "This record was changed by someone else. Reload it and try again.",
                        null, LocalDateTime.now()));
    }

    /**
     * Handles a {@code ?sort=} on a property the entity doesn't have.
     *
     * @param ex the thrown exception, naming the unknown property
     * @return 400 Bad Request naming the invalid sort property
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleBadSortProperty(PropertyReferenceException ex) {
        log.warn("Invalid sort property: {}", ex.getPropertyName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Invalid sort property '" + ex.getPropertyName() + "'",
                        null, LocalDateTime.now()));
    }

    /**
     * Catch-all fallback for any exception not handled more specifically
     * above. Logs the full stack trace and never leaks internal details to
     * the client.
     *
     * @param ex the unexpected exception
     * @return 500 Internal Server Error with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal server error", null, LocalDateTime.now()));
    }

    /**
     * Uniform error body returned by every handler in this class.
     *
     * @param status    the HTTP status code
     * @param message   a human-readable summary of the error
     * @param details   optional extra detail (e.g. per-field validation errors), or {@code null}
     * @param timestamp when the error was handled
     */
    public record ErrorResponse(int status, String message, Object details, LocalDateTime timestamp) {}
}

package com.nettdet.backend.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

/**
 * Centralized exception handling component for the application.
 * 
 * <p>This advice captures various exceptions thrown by the service or controller layers
 * and transforms them into standardized {@link ResponseEntity} objects with appropriate
 * HTTP status codes and error messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors triggered by {@code @Valid} or {@code @Validated} annotations.
     * Extracts field-specific error messages and returns a 400 Bad Request status.
     *
     * @param ex the validation exception
     * @return a map of field names to error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles illegal argument exceptions, typically representing client errors.
     * Returns a 400 Bad Request status with the exception message.
     *
     * @param ex the illegal argument exception
     * @return a map containing the error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles exceptions where a requested JPA entity could not be found.
     * Returns a 404 Not Found status.
     *
     * @param ex the entity not found exception
     * @return a map containing the error message
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles exceptions where an expected element is missing.
     * Returns a 404 Not Found status.
     *
     * @param ex the no such element exception
     * @return a map containing the error message
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles cases where the application is in an inconsistent state.
     * Returns a 500 Internal Server Error status.
     *
     * @param ex the illegal state exception
     * @return a map containing the error message
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles security access denials for authenticated users.
     * Returns a 403 Forbidden status.
     *
     * @param ex the access denied exception
     * @return a map containing the error message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles authentication failures due to incorrect credentials.
     * Returns a 401 Unauthorized status.
     *
     * @param ex the bad credentials exception
     * @return a map containing the error message
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles database constraint violations, such as duplicate keys.
     * Returns a 409 Conflict status.
     *
     * @param ex the data integrity violation exception
     * @return a map containing a generic conflict message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Data conflict: duplicate or invalid reference"));
    }
}

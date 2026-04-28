package com.nettdet.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.persistence.EntityNotFoundException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @DisplayName("Returns 400 with field-error map for MethodArgumentNotValidException")
    @Test
    void handleValidation_returnsBadRequestWithFieldErrorsMap() {
        FieldError fieldError = new FieldError("requestObject", "email", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("must not be blank", response.getBody().get("email"));
    }

    @DisplayName("Returns 400 with the exception message for IllegalArgumentException")
    @Test
    void handleBadRequest_returnsBadRequestWithExceptionMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().get("error"));
    }

    @DisplayName("Returns 404 with the exception message for EntityNotFoundException")
    @Test
    void handleNotFound_returnsNotFoundWithExceptionMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(new EntityNotFoundException("not here"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("not here", response.getBody().get("error"));
    }

    @DisplayName("Returns 404 with the exception message for NoSuchElementException")
    @Test
    void handleNoSuchElement_returnsNotFoundWithExceptionMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNoSuchElement(new NoSuchElementException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", response.getBody().get("error"));
    }

    @DisplayName("Returns 500 with the exception message for IllegalStateException")
    @Test
    void handleIllegalState_returnsInternalServerErrorWithExceptionMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalState(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("boom", response.getBody().get("error"));
    }

    @DisplayName("Returns 403 with the exception message for AccessDeniedException")
    @Test
    void handleForbidden_returnsForbiddenWithExceptionMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleForbidden(new AccessDeniedException("nope"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("nope", response.getBody().get("error"));
    }

    @DisplayName("Returns 401 with the exception message for BadCredentialsException")
    @Test
    void handleUnauthorized_returnsUnauthorizedWithExceptionMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUnauthorized(new BadCredentialsException("bad creds"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("bad creds", response.getBody().get("error"));
    }

    @DisplayName("Returns 409 with a generic message for DataIntegrityViolationException")
    @Test
    void handleConflict_returnsConflictWithGenericMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleConflict(new DataIntegrityViolationException("fk violation"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Data conflict: duplicate or invalid reference", response.getBody().get("error"));
    }
}

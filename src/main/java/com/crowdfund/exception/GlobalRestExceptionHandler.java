package com.crowdfund.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Centralized exception advice for REST and Webhook endpoints.
 * Sanitizes errors and never leaks internal stack traces to clients.
 */
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", "Access denied: insufficient permissions.", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", "Invalid input parameters", fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        String errorRef = "ERR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Internal Server Error [Ref: {}]: {}", errorRef, ex.getMessage(), ex);

        // Hide sensitive internal error details from the response
        String userFriendlyMessage = "An unexpected error occurred. Error reference: " + errorRef;
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", userFriendlyMessage, null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error,
                                                              String message, Object details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return new ResponseEntity<>(body, status);
    }
}

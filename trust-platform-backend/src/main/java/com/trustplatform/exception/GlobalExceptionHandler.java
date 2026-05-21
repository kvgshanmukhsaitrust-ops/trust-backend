package com.trustplatform.exception;

import com.trustplatform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==============================
    // Validation Errors (@Valid DTO)
    // ==============================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiResponse<Object> response = ApiResponse.error(
                "Validation failed",
                errors,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // ==============================
    // JSON / Http Body Parsing Errors
    // ==============================

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Failed to read HTTP message / request body: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
                "Malformed request payload or missing required request body",
                null,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // ==============================
    // Database / Integrity Constraints
    // ==============================

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Database constraint or integrity violation", ex);

        String errorMessage = "Database integrity violation. Please ensure all required fields are filled correctly.";
        if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null) {
            String rootMsg = ex.getRootCause().getMessage();
            if (rootMsg.contains("Data too long for column")) {
                errorMessage = "Input data is too long for one of the fields.";
            } else if (rootMsg.contains("cannot be null")) {
                errorMessage = "A required database field is missing or null.";
            } else if (rootMsg.contains("foreign key constraint fails")) {
                errorMessage = "Database relationship integrity check failed (invalid foreign key references).";
            } else if (rootMsg.contains("Duplicate entry")) {
                errorMessage = "An entry with this unique value already exists.";
            }
        }

        ApiResponse<Object> response = ApiResponse.error(
                errorMessage,
                null,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // ==============================
    // Custom Exceptions
    // ==============================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicate(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {

        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // ==============================
    // Spring Security
    // ==============================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        return buildResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    // ==============================
    // Fallback (Critical)
    // ==============================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception occurred", ex);

        ApiResponse<Object> response = ApiResponse.error(
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ==============================
    // Helper
    // ==============================

    private ResponseEntity<ApiResponse<Object>> buildResponse(
            String message,
            HttpStatus status) {

        ApiResponse<Object> response = ApiResponse.error(
                message,
                null,
                status.value()
        );

        return ResponseEntity.status(status).body(response);
    }
}
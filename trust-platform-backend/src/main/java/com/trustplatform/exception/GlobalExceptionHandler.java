package com.trustplatform.exception;

import com.trustplatform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.trustplatform.audit.AuditService;
import com.trustplatform.notification.NotificationService;
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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuditService auditService;
    private final NotificationService notificationService;

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

        // Operational Audit capture for validation failures
        String details = "Validation failed for URI: " + request.getRequestURI() + 
                         ". Invalid fields: " + errors.keySet().toString() + 
                         ". Detailed errors: " + errors.toString();
        logFailedAudit("VALIDATION_FAILURE", "Validation", details, "Validation failed", request);

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

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex,
            HttpServletRequest request) {

        return buildResponse("Static resource not found: " + ex.getResourcePath(), HttpStatus.NOT_FOUND);
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

        logFailedAudit("LOGIN_FAILED", "Auth", "Failed login attempt with invalid credentials.", ex.getMessage(), request);
        return buildResponse("Invalid email or password", HttpStatus.UNAUTHORIZED);
    }

    // ==============================
    // Security Violations (Access Denied)
    // ==============================

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {

        String details = "Access denied for URI: " + request.getRequestURI() + 
                         " [" + request.getMethod() + "]. User lacks required authority.";
        logFailedAudit("SECURITY_VIOLATION", "Security", details, ex.getMessage(), request);

        ApiResponse<Object> response = ApiResponse.error(
                "Access denied. You do not have permission to perform this action.",
                HttpStatus.FORBIDDEN.value()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==============================
    // Audit Logging Helpers
    // ==============================

    private void logFailedAudit(String action, String targetResource, String details, String errorMsg, HttpServletRequest request) {
        try {
            String performedBy = "anonymous";
            if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null &&
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                performedBy = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            }
            
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 497) + "...";
            }
            
            String ipAddress = "127.0.0.1";
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.trim().isEmpty()) {
                ipAddress = xfHeader.split(",")[0].trim();
            } else {
                ipAddress = request.getRemoteAddr();
            }
            
            // Mask raw credentials to prevent session leaks
            String sanitizedDetails = sanitize(details);
            String sanitizedErrorMsg = sanitize(errorMsg);
            
            auditService.log(action, performedBy, targetResource, sanitizedDetails, ipAddress, userAgent, "FAILED", sanitizedErrorMsg);

            // Trigger real-time WebSocket alert to Admins upon unauthorized access attempts
            if ("SECURITY_VIOLATION".equals(action)) {
                notificationService.sendToAdmins("Security Violation Warning", 
                        "Security warning: Unauthorized access attempt blocked at URI: " + request.getRequestURI() + " [" + request.getMethod() + "] for user: " + performedBy, "SYSTEM");
            }
        } catch (Exception e) {
            log.error("Failed to write global exception handler audit log", e);
        }
    }

    private String sanitize(String input) {
        if (input == null) return null;
        // Case-insensitive regex masking for keys
        String masked = input.replaceAll("(?i)(\"(?:password|passwordConfirm|token|secret|refreshToken|mailPassword|razorpaySecret)\"\\s*:\\s*\")[^\"]+(\")", "$1[MASKED]$2");
        masked = masked.replaceAll("(?i)(\"(?:password|passwordConfirm|token|secret|refreshToken|mailPassword|razorpaySecret)\"\\s*:\\s*)[^,}\\s]+", "$1\"[MASKED]\"");
        return masked;
    }

    // ==============================
    // Fallback (Critical)
    // ==============================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception occurred", ex);

        String errMsg = "An unexpected error occurred";
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            errMsg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        ApiResponse<Object> response = ApiResponse.error(
                errMsg,
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
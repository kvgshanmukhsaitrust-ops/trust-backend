package com.trustplatform.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditAction)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        String actionName = auditAction.value();
        String performedBy = "anonymous";
        
        // 1. Resolve Actor Email
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            performedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        // 1b. Resolve Target Resource from target class name dynamically
        String targetResource = "Unknown";
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            targetResource = className.replace("Controller", "").replace("Service", "").replace("Aspect", "");
        } catch (Exception e) {
            log.warn("Failed to resolve target resource name from class signature: {}", e.getMessage());
        }

        // 2. Resolve Client Context (IP Address and User-Agent)
        String ipAddress = "127.0.0.1";
        String userAgent = "System/Unknown";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 497) + "...";
            }
            
            // Handle client IP behind proxies (like Cloudflare, ALB)
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null && !xfHeader.trim().isEmpty()) {
                ipAddress = xfHeader.split(",")[0].trim();
            } else {
                ipAddress = request.getRemoteAddr();
            }
        }

        // 3. Assemble Method Parameter Details (Sanitized)
        String details = "Method invoked: " + joinPoint.getSignature().toShortString();
        try {
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            StringBuilder sb = new StringBuilder("Parameters: ");
            for (int i = 0; i < args.length; i++) {
                if (paramNames != null && i < paramNames.length) {
                    String name = paramNames[i];
                    if (name.equalsIgnoreCase("password") || name.equalsIgnoreCase("token") || name.equalsIgnoreCase("secret")) {
                        sb.append(name).append("=[MASKED]; ");
                    } else {
                        String valueStr = args[i] != null ? objectMapper.writeValueAsString(args[i]) : "null";
                        sb.append(name).append("=").append(sanitize(valueStr)).append("; ");
                    }
                }
            }
            details = sb.toString();
        } catch (Exception e) {
            log.warn("Failed to serialize arguments for audit log, fallback to short signature. Error: {}", e.getMessage());
        }

        // 4. Proceed with Execution and Record Outcome
        Object result;
        try {
            result = joinPoint.proceed();
            
            // Log SUCCESS state
            auditService.log(actionName, performedBy, targetResource, details, ipAddress, userAgent, "SUCCESS", null);
            return result;
        } catch (Throwable throwable) {
            // Log FAILED state with exact exception details
            String errorMsg = throwable.getMessage();
            if (errorMsg != null && errorMsg.length() > 2000) {
                errorMsg = errorMsg.substring(0, 1997) + "...";
            }
            
            auditService.log(actionName, performedBy, targetResource, details, ipAddress, userAgent, "FAILED", errorMsg);
            throw throwable; // Re-throw to preserve core transaction rollbacks and validation handling
        }
    }

    /**
     * Helper to mask sensitive fields like password, token, or secret to prevent leaks
     */
    private String sanitize(String input) {
        if (input == null) return null;
        // Case-insensitive regex masking for keys
        String masked = input.replaceAll("(?i)(\"(?:password|passwordConfirm|token|secret|refreshToken|mailPassword|razorpaySecret)\"\\s*:\\s*\")[^\"]+(\")", "$1[MASKED]$2");
        masked = masked.replaceAll("(?i)(\"(?:password|passwordConfirm|token|secret|refreshToken|mailPassword|razorpaySecret)\"\\s*:\\s*)[^,}\\s]+", "$1\"[MASKED]\"");
        return masked;
    }
}

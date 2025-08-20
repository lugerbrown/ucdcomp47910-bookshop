package com.ucd.bookshop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Security audit logging service for CWE-778 mitigation.
 * Provides centralized security event logging with structured format.
 * Enhanced with CWE-117 log injection protection through input sanitization.
 */
@Service
public class SecurityAuditService {

    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("ACCESS_AUDIT");
    private static final Logger AUTH_LOGGER = LoggerFactory.getLogger("AUTH_AUDIT");

    // CWE-117 protection: Constants for log injection prevention
    private static final String REPLACEMENT_CHAR = "_";
    private static final String NEWLINE_REPLACEMENT = " | ";
    private static final String TAB_REPLACEMENT = " ";
    
    // Control characters and sequences to remove/replace
    private static final char[] CONTROL_CHARS = {
        '\r', '\n', '\t', '\f', '\b', '\u0000', '\u001B', '\u007F'
    };
    
    // Unicode line separator characters
    private static final String[] LINE_SEPARATORS = {
        "\u2028", "\u2029", "\u0085"
    };

    /**
     * Log authentication success events
     */
    public void logAuthenticationSuccess(String username, String clientIp, String userAgent) {
        Map<String, Object> event = createBaseEvent("AUTHENTICATION_SUCCESS");
        event.put("username", sanitizeUsername(username));
        event.put("clientIp", clientIp);
        event.put("userAgent", sanitizeUserAgent(userAgent));
        
        AUTH_LOGGER.info("Authentication successful: {}", formatLogEntry(event));
    }

    /**
     * Log authentication failure events
     */
    public void logAuthenticationFailure(String username, String clientIp, String userAgent, String reason) {
        Map<String, Object> event = createBaseEvent("AUTHENTICATION_FAILURE");
        event.put("username", sanitizeUsername(username));
        event.put("clientIp", clientIp);
        event.put("userAgent", sanitizeUserAgent(userAgent));
        event.put("reason", sanitizeReason(reason));
        
        AUTH_LOGGER.warn("Authentication failed: {}", formatLogEntry(event));
    }

    /**
     * Log account lockout events
     */
    public void logAccountLockout(String username, String clientIp, int attemptCount, String lockDuration) {
        Map<String, Object> event = createBaseEvent("ACCOUNT_LOCKOUT");
        event.put("username", sanitizeUsername(username));
        event.put("clientIp", clientIp);
        event.put("attemptCount", attemptCount);
        event.put("lockDuration", sanitizeLogInput(lockDuration));
        
        SECURITY_LOGGER.warn("Account locked due to excessive login attempts: {}", formatLogEntry(event));
    }

    /**
     * Log IP address lockout events
     */
    public void logIpLockout(String clientIp, int attemptCount, String lockDuration) {
        Map<String, Object> event = createBaseEvent("IP_LOCKOUT");
        event.put("clientIp", clientIp);
        event.put("attemptCount", attemptCount);
        event.put("lockDuration", sanitizeLogInput(lockDuration));
        
        SECURITY_LOGGER.warn("IP address locked due to excessive login attempts: {}", formatLogEntry(event));
    }

    /**
     * Log authorization failure events
     */
    public void logAuthorizationFailure(String resource, String method, String reason) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent("AUTHORIZATION_FAILURE");
        event.put("username", sanitizeUsername(currentUser));
        event.put("resource", sanitizePath(resource));
        event.put("method", sanitizeLogInput(method));
        event.put("reason", sanitizeReason(reason));
        
        ACCESS_LOGGER.warn("Access denied: {}", formatLogEntry(event));
    }

    /**
     * Log session management events
     */
    public void logSessionEvent(String eventType, String sessionId, String details) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent(sanitizeLogInput(eventType));
        event.put("username", sanitizeUsername(currentUser));
        event.put("sessionId", sessionId != null ? sessionId.substring(0, Math.min(8, sessionId.length())) + "..." : "UNKNOWN");
        event.put("details", sanitizeReason(details));
        
        SECURITY_LOGGER.info("Session event: {}", formatLogEntry(event));
    }

    /**
     * Log input validation failures
     */
    public void logValidationFailure(String field, String value, String constraint, String endpoint) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent("VALIDATION_FAILURE");
        event.put("username", sanitizeUsername(currentUser));
        event.put("field", sanitizeLogInput(field));
        event.put("value", sanitizeLogInput(value));
        event.put("constraint", sanitizeLogInput(constraint));
        event.put("endpoint", sanitizePath(endpoint));
        
        SECURITY_LOGGER.warn("Input validation failed: {}", formatLogEntry(event));
    }

    /**
     * Log admin operations
     */
    public void logAdminOperation(String operation, String targetResource, String details) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent("ADMIN_OPERATION");
        event.put("username", sanitizeUsername(currentUser));
        event.put("operation", sanitizeOperation(operation));
        event.put("targetResource", sanitizePath(targetResource));
        event.put("details", sanitizeReason(details));
        
        ACCESS_LOGGER.info("Admin operation performed: {}", formatLogEntry(event));
    }

    /**
     * Log CSRF token validation failures
     */
    public void logCsrfFailure(String clientIp, String userAgent, String endpoint) {
        Map<String, Object> event = createBaseEvent("CSRF_FAILURE");
        event.put("clientIp", clientIp);
        event.put("userAgent", sanitizeUserAgent(userAgent));
        event.put("endpoint", sanitizePath(endpoint));
        
        SECURITY_LOGGER.error("CSRF token validation failed: {}", formatLogEntry(event));
    }

    /**
     * Log rate limiting events
     */
    public void logRateLimitTriggered(String clientIp, String username, String endpoint, int requestCount) {
        Map<String, Object> event = createBaseEvent("RATE_LIMIT_TRIGGERED");
        event.put("clientIp", clientIp);
        event.put("username", sanitizeUsername(username));
        event.put("endpoint", sanitizePath(endpoint));
        event.put("requestCount", requestCount);
        
        SECURITY_LOGGER.warn("Rate limit triggered: {}", formatLogEntry(event));
    }

    /**
     * Log privilege escalation attempts
     */
    public void logPrivilegeEscalation(String username, String attemptedRole, String currentRole, String resource) {
        Map<String, Object> event = createBaseEvent("PRIVILEGE_ESCALATION_ATTEMPT");
        event.put("username", sanitizeUsername(username));
        event.put("attemptedRole", sanitizeLogInput(attemptedRole));
        event.put("currentRole", sanitizeLogInput(currentRole));
        event.put("resource", sanitizePath(resource));
        
        SECURITY_LOGGER.error("Privilege escalation attempt detected: {}", formatLogEntry(event));
    }

    /**
     * Log user registration events
     */
    public void logUserRegistration(String username, String status, String reason) {
        Map<String, Object> event = createBaseEvent("USER_REGISTRATION");
        event.put("username", sanitizeUsername(username));
        event.put("status", sanitizeLogInput(status));
        if (reason != null) {
            event.put("reason", sanitizeReason(reason));
        }
        
        if ("SUCCESS".equals(status)) {
            SECURITY_LOGGER.info("User registration successful: {}", formatLogEntry(event));
        } else {
            SECURITY_LOGGER.warn("User registration failed: {}", formatLogEntry(event));
        }
    }

    /**
     * Create base event structure with common fields
     */
    private Map<String, Object> createBaseEvent(String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("eventType", eventType);
        event.put("applicationName", "BookShop");
        return event;
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // Ignore exceptions in logging context
        }
        return null;
    }

    /**
     * CWE-117 Protection: Comprehensive log input sanitization
     * Prevents log injection attacks by removing/replacing dangerous characters
     */
    private String sanitizeLogInput(String input) {
        if (input == null) {
            return "NULL";
        }
        
        String sanitized = input;
        
        // Limit length to prevent log flooding
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000) + "...";
        }
        
        // Replace CRLF sequences that could break log format
        sanitized = sanitized.replace("\r\n", NEWLINE_REPLACEMENT);
        sanitized = sanitized.replace("\n\r", NEWLINE_REPLACEMENT);
        sanitized = sanitized.replace("\r", NEWLINE_REPLACEMENT);
        sanitized = sanitized.replace("\n", NEWLINE_REPLACEMENT);
        
        // Replace tab characters
        sanitized = sanitized.replace("\t", TAB_REPLACEMENT);
        
        // Remove other control characters
        for (char controlChar : CONTROL_CHARS) {
            sanitized = sanitized.replace(String.valueOf(controlChar), REPLACEMENT_CHAR);
        }
        
        // Remove Unicode line separators
        for (String lineSeparator : LINE_SEPARATORS) {
            sanitized = sanitized.replace(lineSeparator, NEWLINE_REPLACEMENT);
        }
        
        // Remove ANSI escape sequences (commonly used in log injection)
        sanitized = sanitized.replaceAll("\u001B\\[[0-9;]*[a-zA-Z]", "");
        
        // Remove null bytes and other dangerous sequences
        sanitized = sanitized.replace("\u0000", "");
        
        return sanitized;
    }
    
    /**
     * CWE-117 Protection: Sanitize username for logging
     */
    private String sanitizeUsername(String username) {
        if (username == null) {
            return "ANONYMOUS";
        }
        return sanitizeLogInput(username);
    }
    
    /**
     * CWE-117 Protection: Sanitize reason/message for logging
     */
    private String sanitizeReason(String reason) {
        if (reason == null) {
            return "UNSPECIFIED";
        }
        return sanitizeLogInput(reason);
    }
    
    /**
     * CWE-117 Protection: Sanitize file path for logging
     */
    private String sanitizePath(String path) {
        if (path == null) {
            return "UNKNOWN_PATH";
        }
        return sanitizeLogInput(path);
    }
    
    /**
     * CWE-117 Protection: Sanitize operation name for logging
     */
    private String sanitizeOperation(String operation) {
        if (operation == null) {
            return "UNKNOWN_OPERATION";
        }
        return sanitizeLogInput(operation);
    }

    /**
     * Sanitize user agent string for logging (Enhanced for CWE-117)
     */
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null) return "UNKNOWN";
        // Apply comprehensive sanitization and limit length
        String sanitized = sanitizeLogInput(userAgent);
        return sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;
    }

    /**
     * Sanitize input values for logging (Legacy method - now uses comprehensive sanitization)
     */
    private String sanitizeInputValue(String value) {
        return sanitizeLogInput(value);
    }

    /**
     * Format log entry as structured string
     */
    private String formatLogEntry(Map<String, Object> event) {
        StringBuilder sb = new StringBuilder();
        event.forEach((key, value) -> {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append(key).append("=").append(value);
        });
        return sb.toString();
    }
}

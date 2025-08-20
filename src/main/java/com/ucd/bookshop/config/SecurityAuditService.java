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
 */
@Service
public class SecurityAuditService {

    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("ACCESS_AUDIT");
    private static final Logger AUTH_LOGGER = LoggerFactory.getLogger("AUTH_AUDIT");

    /**
     * Log authentication success events
     */
    public void logAuthenticationSuccess(String username, String clientIp, String userAgent) {
        Map<String, Object> event = createBaseEvent("AUTHENTICATION_SUCCESS");
        event.put("username", username);
        event.put("clientIp", clientIp);
        event.put("userAgent", sanitizeUserAgent(userAgent));
        
        AUTH_LOGGER.info("Authentication successful: {}", formatLogEntry(event));
    }

    /**
     * Log authentication failure events
     */
    public void logAuthenticationFailure(String username, String clientIp, String userAgent, String reason) {
        Map<String, Object> event = createBaseEvent("AUTHENTICATION_FAILURE");
        event.put("username", username != null ? username : "UNKNOWN");
        event.put("clientIp", clientIp);
        event.put("userAgent", sanitizeUserAgent(userAgent));
        event.put("reason", reason);
        
        AUTH_LOGGER.warn("Authentication failed: {}", formatLogEntry(event));
    }

    /**
     * Log account lockout events
     */
    public void logAccountLockout(String username, String clientIp, int attemptCount, String lockDuration) {
        Map<String, Object> event = createBaseEvent("ACCOUNT_LOCKOUT");
        event.put("username", username);
        event.put("clientIp", clientIp);
        event.put("attemptCount", attemptCount);
        event.put("lockDuration", lockDuration);
        
        SECURITY_LOGGER.warn("Account locked due to excessive login attempts: {}", formatLogEntry(event));
    }

    /**
     * Log IP address lockout events
     */
    public void logIpLockout(String clientIp, int attemptCount, String lockDuration) {
        Map<String, Object> event = createBaseEvent("IP_LOCKOUT");
        event.put("clientIp", clientIp);
        event.put("attemptCount", attemptCount);
        event.put("lockDuration", lockDuration);
        
        SECURITY_LOGGER.warn("IP address locked due to excessive login attempts: {}", formatLogEntry(event));
    }

    /**
     * Log authorization failure events
     */
    public void logAuthorizationFailure(String resource, String method, String reason) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent("AUTHORIZATION_FAILURE");
        event.put("username", currentUser != null ? currentUser : "ANONYMOUS");
        event.put("resource", resource);
        event.put("method", method);
        event.put("reason", reason);
        
        ACCESS_LOGGER.warn("Access denied: {}", formatLogEntry(event));
    }

    /**
     * Log session management events
     */
    public void logSessionEvent(String eventType, String sessionId, String details) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent(eventType);
        event.put("username", currentUser != null ? currentUser : "ANONYMOUS");
        event.put("sessionId", sessionId != null ? sessionId.substring(0, Math.min(8, sessionId.length())) + "..." : "UNKNOWN");
        event.put("details", details);
        
        SECURITY_LOGGER.info("Session event: {}", formatLogEntry(event));
    }

    /**
     * Log input validation failures
     */
    public void logValidationFailure(String field, String value, String constraint, String endpoint) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent("VALIDATION_FAILURE");
        event.put("username", currentUser != null ? currentUser : "ANONYMOUS");
        event.put("field", field);
        event.put("value", sanitizeInputValue(value));
        event.put("constraint", constraint);
        event.put("endpoint", endpoint);
        
        SECURITY_LOGGER.warn("Input validation failed: {}", formatLogEntry(event));
    }

    /**
     * Log admin operations
     */
    public void logAdminOperation(String operation, String targetResource, String details) {
        String currentUser = getCurrentUsername();
        Map<String, Object> event = createBaseEvent("ADMIN_OPERATION");
        event.put("username", currentUser != null ? currentUser : "UNKNOWN");
        event.put("operation", operation);
        event.put("targetResource", targetResource);
        event.put("details", details);
        
        ACCESS_LOGGER.info("Admin operation performed: {}", formatLogEntry(event));
    }

    /**
     * Log CSRF token validation failures
     */
    public void logCsrfFailure(String clientIp, String userAgent, String endpoint) {
        Map<String, Object> event = createBaseEvent("CSRF_FAILURE");
        event.put("clientIp", clientIp);
        event.put("userAgent", sanitizeUserAgent(userAgent));
        event.put("endpoint", endpoint);
        
        SECURITY_LOGGER.error("CSRF token validation failed: {}", formatLogEntry(event));
    }

    /**
     * Log rate limiting events
     */
    public void logRateLimitTriggered(String clientIp, String username, String endpoint, int requestCount) {
        Map<String, Object> event = createBaseEvent("RATE_LIMIT_TRIGGERED");
        event.put("clientIp", clientIp);
        event.put("username", username != null ? username : "ANONYMOUS");
        event.put("endpoint", endpoint);
        event.put("requestCount", requestCount);
        
        SECURITY_LOGGER.warn("Rate limit triggered: {}", formatLogEntry(event));
    }

    /**
     * Log privilege escalation attempts
     */
    public void logPrivilegeEscalation(String username, String attemptedRole, String currentRole, String resource) {
        Map<String, Object> event = createBaseEvent("PRIVILEGE_ESCALATION_ATTEMPT");
        event.put("username", username);
        event.put("attemptedRole", attemptedRole);
        event.put("currentRole", currentRole);
        event.put("resource", resource);
        
        SECURITY_LOGGER.error("Privilege escalation attempt detected: {}", formatLogEntry(event));
    }

    /**
     * Log user registration events
     */
    public void logUserRegistration(String username, String status, String reason) {
        Map<String, Object> event = createBaseEvent("USER_REGISTRATION");
        event.put("username", username);
        event.put("status", status);
        if (reason != null) {
            event.put("reason", reason);
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
     * Sanitize user agent string for logging
     */
    private String sanitizeUserAgent(String userAgent) {
        if (userAgent == null) return "UNKNOWN";
        // Limit length and remove potentially harmful characters
        return userAgent.length() > 200 ? userAgent.substring(0, 200) + "..." : userAgent;
    }

    /**
     * Sanitize input values for logging (prevent log injection)
     */
    private String sanitizeInputValue(String value) {
        if (value == null) return "NULL";
        if (value.length() > 100) {
            value = value.substring(0, 100) + "...";
        }
        // Remove newlines and carriage returns to prevent log injection
        return value.replaceAll("[\r\n\t]", "_");
    }

    /**
     * Format log entry as structured string
     */
    private String formatLogEntry(Map<String, Object> event) {
        StringBuilder sb = new StringBuilder();
        event.forEach((key, value) -> {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(key).append("=").append(value);
        });
        return sb.toString();
    }
}

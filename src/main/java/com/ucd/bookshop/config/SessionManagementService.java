package com.ucd.bookshop.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service for managing session security controls and CWE-613 mitigation.
 * Enhanced with security audit logging for CWE-778 mitigation.
 * Provides centralized session management including timeout validation,
 * session cleanup, and security event handling.
 */
@Service
public class SessionManagementService {

    private static final int MAX_SESSION_AGE_HOURS = 24; // Maximum absolute session lifetime
    private static final int WARNING_THRESHOLD_MINUTES = 5; // Warn user before session expires
    
    private final SecurityAuditService auditService;

    public SessionManagementService(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Validates if the current session is still valid based on timeout and age.
     * @return true if session is valid, false if it should be invalidated
     */
    public boolean isSessionValid() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return false;
        }

        try {
            // Check if session has exceeded maximum age
            long sessionAgeHours = ChronoUnit.HOURS.between(
                Instant.ofEpochMilli(session.getCreationTime()),
                Instant.now()
            );
            
            if (sessionAgeHours >= MAX_SESSION_AGE_HOURS) {
                auditService.logSessionEvent("SESSION_EXPIRED_MAX_AGE", session.getId(), 
                    "Session exceeded maximum age of " + MAX_SESSION_AGE_HOURS + " hours");
                return false;
            }

            // Check if session has exceeded inactivity timeout
            long inactiveMinutes = ChronoUnit.MINUTES.between(
                Instant.ofEpochMilli(session.getLastAccessedTime()),
                Instant.now()
            );
            
            if (inactiveMinutes >= 30) { // 30 minute timeout as configured
                auditService.logSessionEvent("SESSION_EXPIRED_INACTIVITY", session.getId(), 
                    "Session exceeded inactivity timeout of 30 minutes");
                return false;
            }

            return true;
        } catch (IllegalStateException e) {
            // Session already invalidated
            auditService.logSessionEvent("SESSION_ALREADY_INVALIDATED", "UNKNOWN", 
                "Attempted to check invalid session");
            return false;
        }
    }

    /**
     * Refreshes the current session by updating last access time.
     * This is automatically handled by servlet container, but can be called explicitly.
     */
    public void refreshSession() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            try {
                // Access the session to update last access time
                session.getLastAccessedTime();
                auditService.logSessionEvent("SESSION_REFRESHED", session.getId(), 
                    "Session explicitly refreshed by user request");
            } catch (IllegalStateException e) {
                auditService.logSessionEvent("SESSION_REFRESH_FAILED", "UNKNOWN", 
                    "Attempted to refresh invalid session");
            }
        }
    }

    /**
     * Invalidates the current session and clears security context.
     * Used for security events like password changes or suspicious activity.
     */
    public void invalidateSession() {
        HttpSession session = getCurrentSession();
        String sessionId = session != null ? session.getId() : "UNKNOWN";
        
        if (session != null) {
            try {
                session.invalidate();
                auditService.logSessionEvent("SESSION_INVALIDATED", sessionId, 
                    "Session manually invalidated");
            } catch (IllegalStateException e) {
                auditService.logSessionEvent("SESSION_ALREADY_INVALIDATED", sessionId, 
                    "Attempted to invalidate already invalid session");
            }
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Checks if session is approaching expiration and returns warning status.
     * @return true if session will expire soon, false otherwise
     */
    public boolean isSessionExpiringSoon() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return false;
        }

        long lastAccessMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(session.getLastAccessedTime()),
            Instant.now()
        );
        
        boolean expiringSoon = lastAccessMinutes >= (30 - WARNING_THRESHOLD_MINUTES);
        
        if (expiringSoon) {
            auditService.logSessionEvent("SESSION_EXPIRING_SOON", session.getId(), 
                "Session will expire in " + (30 - lastAccessMinutes) + " minutes");
        }
        
        return expiringSoon;
    }

    /**
     * Gets remaining session time in minutes.
     * @return remaining minutes or 0 if session invalid
     */
    public long getRemainingSessionTimeMinutes() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return 0;
        }

        long lastAccessMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(session.getLastAccessedTime()),
            Instant.now()
        );
        
        return Math.max(0, 30 - lastAccessMinutes);
    }

    /**
     * Gets the current HTTP session from the request context.
     * @return current session or null if not available
     */
    private HttpSession getCurrentSession() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null && attributes.getRequest() != null) {
                return attributes.getRequest().getSession(false);
            }
        } catch (Exception e) {
            auditService.logSessionEvent("SESSION_CONTEXT_ERROR", "UNKNOWN", 
                "Error accessing session context: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets the current authenticated user's username.
     * @return username or null if not authenticated
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}

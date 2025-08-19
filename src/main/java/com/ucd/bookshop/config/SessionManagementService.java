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
 * Provides centralized session management including timeout validation,
 * session cleanup, and security event handling.
 */
@Service
public class SessionManagementService {

    private static final int MAX_SESSION_AGE_HOURS = 24; // Maximum absolute session lifetime
    private static final int WARNING_THRESHOLD_MINUTES = 5; // Warn user before session expires

    /**
     * Validates if the current session is still valid based on timeout and age.
     * @return true if session is valid, false if it should be invalidated
     */
    public boolean isSessionValid() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return false;
        }

        // Check session age (absolute timeout)
        long sessionAgeHours = ChronoUnit.HOURS.between(
            Instant.ofEpochMilli(session.getCreationTime()),
            Instant.now()
        );
        
        if (sessionAgeHours >= MAX_SESSION_AGE_HOURS) {
            invalidateSession();
            return false;
        }

        // Check last access time (inactivity timeout)
        long lastAccessMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(session.getLastAccessedTime()),
            Instant.now()
        );
        
        if (lastAccessMinutes >= 30) { // 30 minutes inactivity timeout
            invalidateSession();
            return false;
        }

        return true;
    }

    /**
     * Invalidates the current session and clears security context.
     * Used for security events like password changes or suspicious activity.
     */
    public void invalidateSession() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.invalidate();
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
        
        return lastAccessMinutes >= (30 - WARNING_THRESHOLD_MINUTES);
    }

    /**
     * Gets the remaining session time in minutes.
     * @return remaining time in minutes, or -1 if session is invalid
     */
    public long getRemainingSessionTimeMinutes() {
        HttpSession session = getCurrentSession();
        if (session == null) {
            return -1;
        }

        long lastAccessMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(session.getLastAccessedTime()),
            Instant.now()
        );
        
        return Math.max(0, 30 - lastAccessMinutes);
    }

    /**
     * Refreshes the session by updating last access time.
     * Should be called on authenticated requests to extend session validity.
     */
    public void refreshSession() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            // Touch the session to update last access time
            session.setAttribute("lastRefresh", Instant.now());
        }
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
            // Log error if needed, but don't fail
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

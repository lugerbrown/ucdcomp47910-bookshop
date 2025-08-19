package com.ucd.bookshop.controller;

import com.ucd.bookshop.config.SessionManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for session management endpoints to support CWE-613 mitigation.
 * Provides session status checking and session extension capabilities.
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionManagementService sessionManagementService;

    public SessionController(SessionManagementService sessionManagementService) {
        this.sessionManagementService = sessionManagementService;
    }

    /**
     * Get current session status including expiration information.
     * @param principal authenticated user details
     * @return session status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Map<String, Object> status = new HashMap<>();
        status.put("username", principal.getUsername());
        status.put("authenticated", true);
        status.put("sessionValid", sessionManagementService.isSessionValid());
        status.put("expiringSoon", sessionManagementService.isSessionExpiringSoon());
        status.put("remainingMinutes", sessionManagementService.getRemainingSessionTimeMinutes());

        return ResponseEntity.ok(status);
    }

    /**
     * Extend the current session by refreshing it.
     * @param principal authenticated user details
     * @return success/error response
     */
    @PostMapping("/extend")
    public ResponseEntity<Map<String, Object>> extendSession(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (!sessionManagementService.isSessionValid()) {
            return ResponseEntity.status(401).body(Map.of("error", "Session expired"));
        }

        // Refresh the session
        sessionManagementService.refreshSession();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Session extended successfully");
        response.put("remainingMinutes", sessionManagementService.getRemainingSessionTimeMinutes());

        return ResponseEntity.ok(response);
    }

    /**
     * Invalidate the current session (logout).
     * @param principal authenticated user details
     * @return success response
     */
    @PostMapping("/invalidate")
    public ResponseEntity<Map<String, Object>> invalidateSession(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        // Invalidate the session
        sessionManagementService.invalidateSession();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Session invalidated successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get session configuration information.
     * @return session configuration details
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSessionConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("inactivityTimeoutMinutes", 30);
        config.put("maxSessionAgeHours", 24);
        config.put("warningThresholdMinutes", 5);
        config.put("maxConcurrentSessions", 1);

        return ResponseEntity.ok(config);
    }
}

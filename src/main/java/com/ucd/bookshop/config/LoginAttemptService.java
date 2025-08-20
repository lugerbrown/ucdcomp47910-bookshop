package com.ucd.bookshop.config;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks authentication failures per username and per client IP to mitigate brute force (CWE-307).
 * Enhanced with security audit logging for CWE-778 mitigation.
 * Simple in-memory implementation suitable for single-instance deployment / demo.
 * For clustered deployments this should be replaced with a distributed cache (e.g. Redis) and
 * adaptive algorithms / anomaly detection.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_USERNAME_ATTEMPTS = 5;             // threshold before lock
    private static final int MAX_IP_ATTEMPTS = 30;                   // broader spray threshold
    private static final Duration WINDOW = Duration.ofMinutes(15);  // rolling attempt window
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10); // lock period

    private final Map<String, AttemptWindow> userAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptWindow> ipAttempts = new ConcurrentHashMap<>();
    private final SecurityAuditService auditService;

    public LoginAttemptService(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    public void recordFailure(String username, String ip) {
        if (username != null && !username.isBlank()) {
            AttemptWindow oldWindow = userAttempts.get(username.toLowerCase());
            AttemptWindow newWindow = userAttempts.compute(username.toLowerCase(), (k, aw) -> AttemptWindow.failed(aw));
            
            // Log lockout event if this failure triggers a lock
            if (newWindow.isLocked(Instant.now()) && (oldWindow == null || !oldWindow.isLocked(Instant.now()))) {
                auditService.logAccountLockout(username, ip, newWindow.failures, LOCK_DURATION.toString());
            }
        }
        if (ip != null && !ip.isBlank()) {
            AttemptWindow oldWindow = ipAttempts.get(ip);
            AttemptWindow newWindow = ipAttempts.compute(ip, (k, aw) -> AttemptWindow.failed(aw));
            
            // Log IP lockout event if this failure triggers a lock
            if (newWindow.isLocked(Instant.now()) && (oldWindow == null || !oldWindow.isLocked(Instant.now()))) {
                auditService.logIpLockout(ip, newWindow.failures, LOCK_DURATION.toString());
            }
        }
    }

    public void recordSuccess(String username, String ip) {
        if (username != null && !username.isBlank()) {
            userAttempts.remove(username.toLowerCase());
        }
        if (ip != null && !ip.isBlank()) {
            // Optionally retain IP attempt history for spray detection.
        }
    }

    public boolean isBlocked(String username, String ip) {
        Instant now = Instant.now();
        if (username != null && !username.isBlank()) {
            AttemptWindow aw = userAttempts.get(username.toLowerCase());
            if (aw != null && aw.isLocked(now)) return true;
        }
        if (ip != null && !ip.isBlank()) {
            AttemptWindow aw = ipAttempts.get(ip);
            if (aw != null && aw.isLocked(now)) return true;
        }
        return false;
    }

    private static final class AttemptWindow {
        final int failures;
        final Instant firstFailure;
        final Instant lockUntil; // null if not locked

        AttemptWindow(int failures, Instant firstFailure, Instant lockUntil) {
            this.failures = failures;
            this.firstFailure = firstFailure;
            this.lockUntil = lockUntil;
        }

        static AttemptWindow failed(AttemptWindow previous) {
            Instant now = Instant.now();
            if (previous == null) {
                return new AttemptWindow(1, now, null);
            }
            if (previous.lockUntil == null && now.isAfter(previous.firstFailure.plus(WINDOW))) {
                return new AttemptWindow(1, now, null);
            }
            int newFailures = previous.failures + 1;
            Instant lockUntil = previous.lockUntil;
            if (lockUntil == null) {
                boolean exceedUser = newFailures >= MAX_USERNAME_ATTEMPTS;
                boolean exceedIp = newFailures >= MAX_IP_ATTEMPTS; // For IP map this threshold is larger
                if (exceedUser || exceedIp) {
                    lockUntil = now.plus(LOCK_DURATION);
                }
            } else if (now.isAfter(lockUntil)) {
                return new AttemptWindow(1, now, null);
            }
            return new AttemptWindow(newFailures, previous.firstFailure, lockUntil);
        }

        boolean isLocked(Instant now) {
            return lockUntil != null && now.isBefore(lockUntil);
        }
    }
}

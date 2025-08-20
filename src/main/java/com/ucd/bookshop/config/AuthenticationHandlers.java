package com.ucd.bookshop.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication success & failure handlers that integrate with LoginAttemptService
 * and SecurityAuditService for CWE-307 and CWE-778 mitigation.
 */
@Component
public class AuthenticationHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final LoginAttemptService attemptService;
    private final SecurityAuditService auditService;

    public AuthenticationHandlers(LoginAttemptService attemptService, SecurityAuditService auditService) {
        this.attemptService = attemptService;
        this.auditService = auditService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String clientIp = clientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        // Record success in attempt service
        attemptService.recordSuccess(username, clientIp);
        
        // Log security event
        auditService.logAuthenticationSuccess(username, clientIp, userAgent);
        
        response.sendRedirect("/");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String clientIp = clientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String reason = exception.getClass().getSimpleName();
        
        // Record failure in attempt service
        attemptService.recordFailure(username, clientIp);
        
        // Log security event
        auditService.logAuthenticationFailure(username, clientIp, userAgent, reason);
        
        response.sendRedirect("/login?error");
    }

    private String clientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            int comma = xf.indexOf(',');
            return comma > -1 ? xf.substring(0, comma).trim() : xf.trim();
        }
        return request.getRemoteAddr();
    }
}

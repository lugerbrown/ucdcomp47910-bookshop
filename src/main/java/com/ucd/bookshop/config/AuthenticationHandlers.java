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
 * Custom authentication success & failure handlers that integrate with LoginAttemptService.
 */
@Component
public class AuthenticationHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final LoginAttemptService attemptService;

    public AuthenticationHandlers(LoginAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        attemptService.recordSuccess(username, clientIp(request));
        response.sendRedirect("/");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        attemptService.recordFailure(username, clientIp(request));
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

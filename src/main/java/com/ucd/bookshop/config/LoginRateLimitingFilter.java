package com.ucd.bookshop.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter that blocks authentication processing if username or IP is currently locked.
 */
@Component
@Order(1)
public class LoginRateLimitingFilter extends OncePerRequestFilter {

    private final LoginAttemptService attemptService;

    public LoginRateLimitingFilter(LoginAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {
        if (isLoginAttempt(request)) {
            String username = request.getParameter("username");
            String ip = clientIp(request);
            if (attemptService.isBlocked(username, ip)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("text/plain;charset=UTF-8");
                response.getOutputStream().write("Too many login attempts. Please try again later.".getBytes(StandardCharsets.UTF_8));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLoginAttempt(HttpServletRequest request) {
        return "/login".equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod());
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

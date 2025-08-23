package com.ucd.bookshop.config;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that validates session validity on each request to mitigate CWE-613.
 * Automatically invalidates expired sessions and redirects users appropriately.
 */
@Component
@Order(2) // Run after LoginRateLimitingFilter but before authentication
public class SessionValidationFilter extends OncePerRequestFilter {

    private final SessionManagementService sessionManagementService;

    public SessionValidationFilter(SessionManagementService sessionManagementService) {
        this.sessionManagementService = sessionManagementService;
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && (origin.startsWith("http://localhost") || origin.startsWith("https://localhost"))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With");
        }
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request, 
                                  @Nonnull HttpServletResponse response, 
                                  @Nonnull FilterChain filterChain) throws ServletException, IOException {
        
        // Skip session validation for public endpoints
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate session for authenticated endpoints
        if (!sessionManagementService.isSessionValid()) {
            // Session is invalid or expired, redirect to login
            addCorsHeaders(request, response);
            response.sendRedirect("/login?expired");
            return;
        }

        // Check if session is expiring soon and add warning header
        if (sessionManagementService.isSessionExpiringSoon()) {
            response.setHeader("X-Session-Expiring", "true");
            response.setHeader("X-Session-Remaining-Minutes", 
                String.valueOf(sessionManagementService.getRemainingSessionTimeMinutes()));
        }

        // Refresh session on valid requests
        sessionManagementService.refreshSession();

        filterChain.doFilter(request, response);
    }

    /**
     * Determines if the request is for a public endpoint that doesn't require session validation.
     */
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Public endpoints that don't require session validation
        return path.equals("/") ||
               path.equals("/login") ||
               path.equals("/register") ||
               path.equals("/test-qr") ||
               path.equals("/css") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/webjars/") ||
               (path.equals("/books") && "GET".equals(method)) ||
               (path.startsWith("/api/books/") && "GET".equals(method)) ||
               (path.equals("/customers/register") && "POST".equals(method));
    }

    @Override
    protected boolean shouldNotFilter(@Nonnull HttpServletRequest request) throws ServletException {
        // Skip filtering for static resources and public endpoints
        String path = request.getRequestURI();
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") || 
               path.startsWith("/webjars/");
    }
}

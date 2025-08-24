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

@Component
@Order(2)
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
        
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!sessionManagementService.isSessionValid()) {
            addCorsHeaders(request, response);
            response.sendRedirect("/login?expired");
            return;
        }

        if (sessionManagementService.isSessionExpiringSoon()) {
            response.setHeader("X-Session-Expiring", "true");
            response.setHeader("X-Session-Remaining-Minutes", 
                String.valueOf(sessionManagementService.getRemainingSessionTimeMinutes()));
        }

        sessionManagementService.refreshSession();

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        return path.equals("/") ||
               path.equals("/login") ||
               path.equals("/register") ||
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
        String path = request.getRequestURI();
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") || 
               path.startsWith("/webjars/");
    }
}

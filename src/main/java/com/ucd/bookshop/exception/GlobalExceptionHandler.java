package com.ucd.bookshop.exception;

import com.ucd.bookshop.config.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private final SecurityAuditService auditService;
    
    public GlobalExceptionHandler(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        Map<String, String> errors = new HashMap<>();
        
        // Log each validation failure for security monitoring
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
            
            // Log validation failure for potential malicious input detection
            auditService.logValidationFailure(
                fe.getField(), 
                fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : "null",
                fe.getDefaultMessage(),
                request.getRequestURI()
            );
        }
        
        body.put("errors", errors);
        body.put("message", "Validation failed");
        return ResponseEntity.badRequest().body(body);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        // Log access denied events for security monitoring
        auditService.logAuthorizationFailure(
            request.getRequestURI(),
            request.getMethod(),
            "Access denied: " + ex.getMessage()
        );
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Access Denied");
        body.put("message", "You don't have permission to access this resource");
        body.put("path", request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        // Log authentication failures
        auditService.logAuthenticationFailure(
            "UNKNOWN", 
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            ex.getMessage()
        );
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Authentication Failed");
        body.put("message", "Authentication credentials are invalid");
        body.put("path", request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
    
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        // Log various HTTP status exceptions that might indicate security issues
        if (ex.getStatusCode() == HttpStatus.FORBIDDEN || ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            auditService.logAuthorizationFailure(
                request.getRequestURI(),
                request.getMethod(),
                "HTTP " + ex.getStatusCode().value() + ": " + ex.getReason()
            );
        }
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getStatusCode().value());
        body.put("error", HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase());
        body.put("message", ex.getReason() != null ? ex.getReason() : "An error occurred");
        body.put("path", request.getRequestURI());
        
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, HttpServletRequest request) {
        // Log unexpected exceptions that might indicate security issues
        auditService.logAdminOperation("SYSTEM_ERROR", request.getRequestURI(), 
                "Unexpected exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred");
        body.put("path", request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

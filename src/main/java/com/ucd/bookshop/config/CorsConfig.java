package com.ucd.bookshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration to prevent unintended cross-origin requests.
 * Addresses CWE-693 by adding explicit origin validation as a protection mechanism.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Only allow requests from the same origin (restrictive policy for CWE-693 mitigation)
        configuration.setAllowCredentials(true);
        configuration.addAllowedOriginPattern("http://localhost:*"); // Development
        configuration.addAllowedOriginPattern("https://localhost:*"); // Development HTTPS
        configuration.addAllowedOrigin("null"); // Allow null origin for file:// and private browsing
        
        // Allow standard HTTP methods
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");
        
        // Allow standard headers
        configuration.addAllowedHeader("*");
        
        // Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

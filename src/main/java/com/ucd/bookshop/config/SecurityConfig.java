package com.ucd.bookshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
                private final Environment environment;

                private static final String ROLE_ADMIN = "ADMIN";
                private static final String ROLE_CUSTOMER = "CUSTOMER";
                private static final String ADMIN_PATH = "/admins/**";
                private static final String AUTHORS_PATH = "/authors/**";
                private static final String BOOKS_PATH = "/books/**";
                private static final String API_BOOKS_GLOB = "/api/books/**";
                private static final String USERS_PATH = "/users/**";
                private static final String CUSTOMERS_PATH = "/customers/**";
                private static final String CARTS_PATH = "/carts/**";
                private static final String CART_ITEMS_PATH = "/cart-items/**";

        public SecurityConfig(Environment environment) {
                this.environment = environment;
        }
    @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationHandlers authenticationHandlers, LoginRateLimitingFilter loginRateLimitingFilter) throws Exception {
                http
                        .sessionManagement( c ->
                                c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                 // Explicitly migrate session ID on authentication to prevent session fixation (Spring does this by default, made explicit for auditability)
                                 .sessionFixation(sf -> sf.migrateSession())
                        )
                        .csrf(csrf -> csrf
                                // Re-enable CSRF protection for CWE-693 mitigation
                                .ignoringRequestMatchers("/api/**") // API endpoints can use stateless authentication if needed
                        )
                        .authorizeHttpRequests(auth -> auth
                                        // Allow static resources (CSS, JS, images)
                                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                                        // Allow public pages
                                        .requestMatchers("/register", "/login", "/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                        // Public GET book catalogue (web + API)
                                            .requestMatchers(HttpMethod.GET, "/books", API_BOOKS_GLOB).permitAll()
                                        // Registration API endpoint
                                        .requestMatchers(HttpMethod.POST, "/customers/register").permitAll()
                                        // Restrict admin areas & management pages
                                            .requestMatchers(ADMIN_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(AUTHORS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(BOOKS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.POST, API_BOOKS_GLOB).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.PUT, API_BOOKS_GLOB).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.DELETE, API_BOOKS_GLOB).hasRole(ROLE_ADMIN)
                                        // User & customer data
                                            .requestMatchers(USERS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.GET, CUSTOMERS_PATH).authenticated() // Ownership checked in controller
                                            .requestMatchers(HttpMethod.PUT, CUSTOMERS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.DELETE, CUSTOMERS_PATH).hasRole(ROLE_ADMIN)
                                        // Cart APIs restricted to customers (with ownership checks in controllers)
                                            .requestMatchers(CARTS_PATH, CART_ITEMS_PATH).hasRole(ROLE_CUSTOMER)
                                        .anyRequest().authenticated()
                        )
                        .formLogin(form -> form
                                        .loginPage("/login")
                                        .successHandler(authenticationHandlers)
                                        .failureHandler(authenticationHandlers)
                                        .permitAll()
                        )
                        .logout(logout -> logout
                                        .logoutUrl("/logout")
                                        .logoutSuccessUrl("/")
                                        .permitAll()
                        );

                // Standard security headers (CSP, Referrer-Policy, Permissions-Policy, X-Content-Type-Options)
                http.headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'; upgrade-insecure-requests"))
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> { /* already conditionally added below for prod */ })
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()"))
                        .xssProtection(x -> x.disable()) // modern browsers rely on CSP; avoid legacy heuristic
                        .frameOptions(fo -> fo.deny())
                        .cacheControl(cc -> {})
                        .contentTypeOptions(cto -> {})
                );

                // Enforce HTTPS and add HSTS only when 'prod' profile is active
                boolean prod = false;
                for (String profile : environment.getActiveProfiles()) {
                        if ("prod".equalsIgnoreCase(profile)) { prod = true; break; }
                }
                if (prod) {
                    // Add simple redirect filter to enforce HTTPS without deprecated API usage
                    http.addFilterBefore(new HttpsEnforcementFilter(), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
                    http.headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .preload(true)
                            .maxAgeInSeconds(31536000)));
                }

                // Add login rate limiting filter prior to authentication processing
                http.addFilterBefore(loginRateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

class HttpsEnforcementFilter extends OncePerRequestFilter {
        @Override
                protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                                                                @org.springframework.lang.NonNull HttpServletResponse response,
                                                                                @org.springframework.lang.NonNull FilterChain filterChain)
                        throws ServletException, IOException {
                if (!request.isSecure()) {
                        String host = request.getHeader("Host");
                        if (host == null) {
                                filterChain.doFilter(request, response);
                                return;
                        }
                        String redirect = "https://" + host + request.getRequestURI();
                        String query = request.getQueryString();
                        if (query != null) redirect += "?" + query;
                        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                        response.setHeader("Location", redirect);
                        return;
                }
                filterChain.doFilter(request, response);
        }
}

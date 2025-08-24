package com.ucd.bookshop.config;

import com.ucd.bookshop.authentication.CustomAuthenticationProvider;
import com.ucd.bookshop.authentication.CustomWebAuthenticationDetailsSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfigurationSource;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
                private final Environment environment;
                private final CustomWebAuthenticationDetailsSource customWebAuthenticationDetailsSource;

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

        public SecurityConfig(Environment environment, 
                             CustomWebAuthenticationDetailsSource customWebAuthenticationDetailsSource) {
                this.environment = environment;
                this.customWebAuthenticationDetailsSource = customWebAuthenticationDetailsSource;
        }
    @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationHandlers authenticationHandlers, 
                                              LoginRateLimitingFilter loginRateLimitingFilter, 
                                              CorsConfigurationSource corsConfigurationSource,
                                              AuthenticationManager authenticationManager,
                                              CustomAuthenticationProvider authProvider) throws Exception {
                http
                        .cors(cors -> cors.configurationSource(corsConfigurationSource))
                        .sessionManagement( sessionManagement ->
                                sessionManagement
                                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                        .sessionFixation(sf -> sf.migrateSession())
                                        .invalidSessionUrl("/login?invalid")
                                        .maximumSessions(1)
                                        .expiredUrl("/login?expired")
                        )
                        .csrf(csrf -> csrf
                                .ignoringRequestMatchers("/api/**")
                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        )
                        .authorizeHttpRequests(auth -> auth
                                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                                        .requestMatchers("/register", "/login", "/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                            .requestMatchers(HttpMethod.GET, "/books", API_BOOKS_GLOB).permitAll()
                                        .requestMatchers(HttpMethod.POST, "/customers/register").permitAll()
                                        .requestMatchers("/user/settings", "/user/update/2fa").authenticated()
                                            .requestMatchers(ADMIN_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(AUTHORS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(BOOKS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.POST, API_BOOKS_GLOB).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.PUT, API_BOOKS_GLOB).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.DELETE, API_BOOKS_GLOB).hasRole(ROLE_ADMIN)
                                            .requestMatchers(USERS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.GET, CUSTOMERS_PATH).authenticated()
                                            .requestMatchers(HttpMethod.PUT, CUSTOMERS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(HttpMethod.DELETE, CUSTOMERS_PATH).hasRole(ROLE_ADMIN)
                                            .requestMatchers(CARTS_PATH, CART_ITEMS_PATH).hasRole(ROLE_CUSTOMER)
                                        .anyRequest().authenticated()
                        )
                        .formLogin(form -> form
                                        .loginPage("/login")
                                        .authenticationDetailsSource(customWebAuthenticationDetailsSource)
                                        .successHandler(authenticationHandlers)
                                        .failureHandler(authenticationHandlers)
                                        .permitAll()
                        )
                        .logout(logout -> logout
                                        .logoutUrl("/logout")
                                        .logoutSuccessUrl("/")
                                        .permitAll()
                        );

                http.headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net; img-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self' http://localhost:* https://localhost:*; upgrade-insecure-requests"))
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> { })
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=()"))
                        .xssProtection(x -> x.disable())
                        .frameOptions(fo -> fo.deny())
                        .cacheControl(cc -> {})
                        .contentTypeOptions(cto -> {})
                );

                boolean prod = false;
                for (String profile : environment.getActiveProfiles()) {
                        if ("prod".equalsIgnoreCase(profile)) { prod = true; break; }
                }
                if (prod) {
                    http.addFilterBefore(new HttpsEnforcementFilter(), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
                    http.headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .preload(true)
                            .maxAgeInSeconds(31536000)));
                }

                http.addFilterBefore(loginRateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
                
                http.authenticationProvider(authProvider);
                
                http.authenticationManager(authenticationManager);

                return http.build();
        }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       CustomAuthenticationProvider authProvider) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(authProvider)
                .build();
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

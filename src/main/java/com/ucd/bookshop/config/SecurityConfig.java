package com.ucd.bookshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement( c ->
            c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
             // Explicitly migrate session ID on authentication to prevent session fixation (Spring does this by default, made explicit for auditability)
             .sessionFixation(sf -> sf.migrateSession())
        )
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simplicity, not recommended for production
                .authorizeHttpRequests(auth -> auth
                        // Allow static resources (CSS, JS, images)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // Allow public pages
                        .requestMatchers("/register", "/login", "/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Public GET book catalogue (web + API)
                        .requestMatchers(HttpMethod.GET, "/books", "/api/books/**").permitAll()
                        // Registration API endpoint
                        .requestMatchers(HttpMethod.POST, "/customers/register").permitAll()
                        // Restrict admin areas & management pages
                        .requestMatchers("/admins/**").hasRole("ADMIN")
                        .requestMatchers("/authors/**").hasRole("ADMIN")
                        .requestMatchers("/books/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/books/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasRole("ADMIN")
                        // User & customer data
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/customers/**").authenticated() // Ownership checked in controller
                        .requestMatchers(HttpMethod.PUT, "/customers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/customers/**").hasRole("ADMIN")
                        // Cart APIs restricted to customers (with ownership checks in controllers)
                        .requestMatchers("/carts/**", "/cart-items/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

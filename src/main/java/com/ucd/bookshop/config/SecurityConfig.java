package com.ucd.bookshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement( c ->
                    c.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) )
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simplicity, not recommended for production
                .authorizeHttpRequests(auth -> auth
                        // Allow static resources (CSS, JS, images)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // Allow public pages
                        .requestMatchers("/register", "/login", "/", "/books", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Restrict admin and user areas
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/authors/**").hasRole("ADMIN")
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

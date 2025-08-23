package com.ucd.bookshop.config;

import com.ucd.bookshop.authentication.CustomAuthenticationProvider;
import com.ucd.bookshop.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthenticationConfig {

    @Bean
    public CustomAuthenticationProvider authProvider(
            UserRepository userRepository,
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        return new CustomAuthenticationProvider(userRepository, userDetailsService, passwordEncoder);
    }
}

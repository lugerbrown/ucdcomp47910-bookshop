package com.ucd.bookshop.authentication;

import com.ucd.bookshop.model.User;
import com.ucd.bookshop.repository.UserRepository;
import org.jboss.aerogear.security.otp.Totp;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationProvider(UserRepository userRepository, 
                                      UserDetailsService userDetailsService,
                                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        String username = auth.getName();
        String password = auth.getCredentials().toString();
        
        String verificationCode = ((CustomWebAuthenticationDetails) auth.getDetails())
                .getVerificationCode();

        // Load user details using UserDetailsService
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        
        // Verify password
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // Get user entity for 2FA check
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (user.isUsing2FA()) {
            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                throw new BadCredentialsException("2FA code is required");
            }

            Totp totp = new Totp(user.getSecret());
            if (!isValidLong(verificationCode) || !totp.verify(verificationCode)) {
                throw new BadCredentialsException("Invalid verification code");
            }
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }

    private boolean isValidLong(String code) {
        try {
            Long.parseLong(code);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}

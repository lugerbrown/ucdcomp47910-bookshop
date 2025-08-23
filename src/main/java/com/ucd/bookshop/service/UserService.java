package com.ucd.bookshop.service;

import com.ucd.bookshop.controller.UserController.UserDto;
import com.ucd.bookshop.model.User;
import com.ucd.bookshop.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final TwoFactorAuthService twoFactorAuthService;
    
    public UserService(UserRepository userRepository, TwoFactorAuthService twoFactorAuthService) {
        this.userRepository = userRepository;
        this.twoFactorAuthService = twoFactorAuthService;
    }
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername());
        }
        return null;
    }
    
    public UserDto updateUser2FA(boolean use2FA) {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("User not found or not authenticated");
        }
        
        if (use2FA) {
            // Enable 2FA
            twoFactorAuthService.enableTwoFactorAuth(user);
        } else {
            // Disable 2FA
            twoFactorAuthService.disableTwoFactorAuth(user);
        }
        
        userRepository.save(user);
        return UserDto.from(user);
    }
    
    public String generateQRUrl(UserDto userDto) {
        User user = userRepository.findByUsername(userDto.getUsername());
        if (user != null && user.isUsing2FA()) {
            return twoFactorAuthService.generateQRUrl(user);
        }
        return null;
    }
    
    public void saveUser(User user) {
        userRepository.save(user);
    }
}

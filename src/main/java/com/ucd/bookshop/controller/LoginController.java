package com.ucd.bookshop.controller;

import com.ucd.bookshop.controller.UserController.UserDto;
import com.ucd.bookshop.model.User;
import com.ucd.bookshop.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class LoginController {
    
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    
    private final UserService userService;
    
    public LoginController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/user/settings")
    @PreAuthorize("isAuthenticated()")
    public String userSettings(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            // User not authenticated, redirect to login
            return "redirect:/login?error=notAuthenticated";
        }
        
        model.addAttribute("user", currentUser);
        model.addAttribute("using2FA", currentUser.isUsing2FA());
        return "user-settings";
    }
    
    @PostMapping("/user/update/2fa")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public Map<String, String> modifyUser2FA(@RequestParam("use2FA") boolean use2FA) {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return Map.of(ERROR_KEY, "User not authenticated");
            }
            
            UserDto user = userService.updateUser2FA(use2FA);
            if (use2FA) {
                String qrUrl = userService.generateQRUrl(user);
                return Map.of(MESSAGE_KEY, qrUrl != null ? qrUrl : "Failed to generate QR code");
            }
            return Map.of(MESSAGE_KEY, "2FA disabled");
        } catch (IllegalStateException e) {
            return Map.of(ERROR_KEY, "Authentication required");
        } catch (Exception e) {
            return Map.of(ERROR_KEY, "Failed to update 2FA settings: " + e.getMessage());
        }
    }
} 
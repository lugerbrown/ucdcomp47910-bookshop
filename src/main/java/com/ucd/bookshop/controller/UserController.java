package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.User;
import com.ucd.bookshop.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        return user == null ? null : UserDto.from(user);
    }

    @GetMapping("/by-username/{username}")
    public UserDto getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        return user == null ? null : UserDto.from(user);
    }

    public static class UserDto {
        private Long id;
        private String username;
        private String role;
        private boolean isUsing2FA;
        private String secret;
        
        public static UserDto from(User u) {
            UserDto dto = new UserDto();
            dto.id = u.getId();
            dto.username = u.getUsername();
            dto.role = u.getRole().name();
            dto.isUsing2FA = u.isUsing2FA();
            dto.secret = u.getSecret();
            return dto;
        }
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public boolean isUsing2FA() { return isUsing2FA; }
        public String getSecret() { return secret; }
    }
} 
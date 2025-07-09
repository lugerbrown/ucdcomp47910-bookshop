package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Admin;
import com.ucd.bookshop.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admins")
public class AdminController {
    private AdminRepository adminRepository;

    public AdminController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @GetMapping
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @GetMapping("/{id}")
    public Admin getAdminById(@PathVariable Long id) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if (admin != null) admin.setPassword(null);
        return admin;
    }
} 
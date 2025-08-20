package com.ucd.bookshop.controller;

import com.ucd.bookshop.config.SecurityAuditService;
import com.ucd.bookshop.model.Admin;
import com.ucd.bookshop.repository.AdminRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admins")
public class AdminController {
    private AdminRepository adminRepository;
    private SecurityAuditService auditService;

    public AdminController(AdminRepository adminRepository, SecurityAuditService auditService) {
        this.adminRepository = adminRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Admin> getAllAdmins(@AuthenticationPrincipal UserDetails principal) {
        // Log admin access to admin list - highly sensitive operation
        auditService.logAdminOperation("VIEW_ALL_ADMINS", "admin_list", 
                "Admin accessed complete admin list - CRITICAL SECURITY EVENT");
        
        List<Admin> admins = adminRepository.findAll();
        // Remove passwords from response for security
        admins.forEach(admin -> admin.setPassword(null));
        return admins;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Admin getAdminById(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        // Log admin access to specific admin - highly sensitive operation
        auditService.logAdminOperation("VIEW_ADMIN_BY_ID", "admin/" + id, 
                "Admin accessed specific admin details - CRITICAL SECURITY EVENT");
        
        Admin admin = adminRepository.findById(id).orElse(null);
        if (admin != null) {
            admin.setPassword(null); // Remove password for security
        } else {
            auditService.logAuthorizationFailure("/admins/" + id, "GET", "Admin not found");
        }
        return admin;
    }
} 
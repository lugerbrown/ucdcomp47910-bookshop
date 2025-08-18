package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.model.User;
import com.ucd.bookshop.repository.CustomerRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private CustomerRepository customerRepository;
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public CustomerDto registerCustomer(@Valid @RequestBody CustomerRegistrationDto request) {
        Customer customer = new Customer();
        customer.setUsername(request.getUsername());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setName(request.getName());
        customer.setSurname(request.getSurname());
        customer.setAddress(request.getAddress());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setEmail(request.getEmail());
        customer.setRole(User.Role.CUSTOMER);
        Customer saved = customerRepository.save(customer);
        return CustomerDto.from(saved);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CustomerDto> getAllCustomers() {
    return customerRepository.findAll().stream().map(CustomerDto::from).toList();
    }

    @GetMapping("/{id}")
    public CustomerDto getCustomerById(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer == null) return null;
        enforceOwnershipOrAdmin(customer, principal);
        return CustomerDto.from(customer);
    }

    @GetMapping("/by-username/{username}")
    public CustomerDto getCustomerByUsername(@PathVariable String username, @AuthenticationPrincipal UserDetails principal) {
        Customer customer = customerRepository.findByUsername(username);
        if (customer == null) return null;
        enforceOwnershipOrAdmin(customer, principal);
        return CustomerDto.from(customer);
    }

    private void enforceOwnershipOrAdmin(Customer target, UserDetails principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return;
        if (!target.getUsername().equals(principal.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public static class CustomerDto {
        private Long id;
        private String username;
        private String name;
        private String surname;
        private String email;
        private String role;
        public static CustomerDto from(Customer c) {
            CustomerDto dto = new CustomerDto();
            dto.id = c.getId();
            dto.username = c.getUsername();
            dto.name = c.getName();
            dto.surname = c.getSurname();
            dto.email = c.getEmail();
            dto.role = c.getRole().name();
            return dto;
        }
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getName() { return name; }
        public String getSurname() { return surname; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }

    public static class CustomerRegistrationDto {
        @NotBlank @Size(min = 3, max = 40)
        private String username;
        @NotBlank @Size(min = 8, max = 100)
        private String password;
        @NotBlank @Size(max = 60)
        private String name;
        @NotBlank @Size(max = 60)
        private String surname;
        @NotBlank @Size(max = 120)
        private String address;
        @NotBlank @Pattern(regexp = "^\\+?[0-9\\- ]{7,20}$", message = "Invalid phone number")
        private String phoneNumber;
        @NotBlank @Email @Size(max = 120)
        private String email;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSurname() { return surname; }
        public void setSurname(String surname) { this.surname = surname; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
} 
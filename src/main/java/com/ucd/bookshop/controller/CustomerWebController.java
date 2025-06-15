package com.ucd.bookshop.controller;

import com.ucd.bookshop.dto.CustomerRegistrationDTO;
import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@AllArgsConstructor
public class CustomerWebController {
    private final CustomerRepository customerRepository;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("customer", new CustomerRegistrationDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerCustomer(@ModelAttribute("customer") CustomerRegistrationDTO dto, Model model) {
        // In production, hash the password before saving!
        Customer customer = new Customer();
        customer.setUsername(dto.getUsername());
        customer.setPassword(dto.getPassword());
        customer.setName(dto.getName());
        customer.setSurname(dto.getSurname());
        customer.setDateOfBirth(dto.getDateOfBirth());
        customer.setAddress(dto.getAddress());
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setEmail(dto.getEmail());
        customer.setRole(Customer.Role.CUSTOMER);
        customerRepository.save(customer);
        model.addAttribute("success", true);
        return "register-success";
    }
} 
package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public Customer registerCustomer(@RequestBody Customer customer) {
        customer.setRole(Customer.Role.CUSTOMER);
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        Customer saved = customerRepository.save(customer);
        saved.setPassword(null);
        return saved;
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        List<Customer> customers = customerRepository.findAll();
        customers.forEach(c -> c.setPassword(null));
        return customers;
    }

    @GetMapping("/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer != null) customer.setPassword(null);
        return customer;
    }

    @GetMapping("/by-username/{username}")
    public Customer getCustomerByUsername(@PathVariable String username) {
        Customer customer = customerRepository.findByUsername(username);
        if (customer != null) customer.setPassword(null);
        return customer;
    }
} 
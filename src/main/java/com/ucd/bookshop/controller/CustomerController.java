package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/register")
    public Customer registerCustomer(@RequestBody Customer customer) {
        // In production, hash the password before saving!
        customer.setRole(Customer.Role.CUSTOMER);
        return customerRepository.save(customer);
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
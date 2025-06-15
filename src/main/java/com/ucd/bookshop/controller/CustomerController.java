package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.repository.CustomerRepository;
import com.ucd.bookshop.dto.CustomerRegistrationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/register")
    public CustomerRegistrationDTO registerCustomer(@RequestBody CustomerRegistrationDTO dto) {
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
        Customer saved = customerRepository.save(customer);
        // Map back to DTO (do not include password)
        CustomerRegistrationDTO responseDto = new CustomerRegistrationDTO();
        responseDto.setUsername(saved.getUsername());
        responseDto.setName(saved.getName());
        responseDto.setSurname(saved.getSurname());
        responseDto.setDateOfBirth(saved.getDateOfBirth());
        responseDto.setAddress(saved.getAddress());
        responseDto.setPhoneNumber(saved.getPhoneNumber());
        responseDto.setEmail(saved.getEmail());
        return responseDto;
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
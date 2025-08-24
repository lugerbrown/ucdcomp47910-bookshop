package com.ucd.bookshop.service;

import com.ucd.bookshop.repository.CustomerRepository;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class RegistrationValidationService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>?,./]).{12,128}$"
    );

    private final CustomerRepository customerRepository;

    public RegistrationValidationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public void validate(String username, String rawPassword) {
        if (customerRepository.findByUsername(username) != null) {
            throw new ValidationException("Username already exists");
        }
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new ValidationException("Password must be 12+ chars and include upper, lower, digit, special");
        }
    }
}

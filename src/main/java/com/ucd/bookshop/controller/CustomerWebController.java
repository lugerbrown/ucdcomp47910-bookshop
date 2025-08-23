package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.model.User;
import com.ucd.bookshop.repository.CustomerRepository;
import com.ucd.bookshop.service.RegistrationValidationService;
import com.ucd.bookshop.service.TwoFactorAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CustomerWebController {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationValidationService registrationValidationService;
    private final TwoFactorAuthService twoFactorAuthService;

    public CustomerWebController(CustomerRepository customerRepository, PasswordEncoder passwordEncoder, 
                               RegistrationValidationService registrationValidationService,
                               TwoFactorAuthService twoFactorAuthService) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationValidationService = registrationValidationService;
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("customerForm", new CustomerForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerCustomer(@Valid @ModelAttribute("customerForm") CustomerForm form, Model model) {
        registrationValidationService.validate(form.getUsername(), form.getPassword());
        Customer c = new Customer();
        c.setUsername(form.getUsername());
        c.setPassword(passwordEncoder.encode(form.getPassword()));
        c.setName(form.getName());
        c.setSurname(form.getSurname());
        c.setAddress(form.getAddress());
        c.setPhoneNumber(form.getPhoneNumber());
        c.setEmail(form.getEmail());
        c.setRole(User.Role.CUSTOMER);
        
        // Handle 2FA setup if user chose to enable it
        if (form.isUsing2FA()) {
            twoFactorAuthService.enableTwoFactorAuth(c);
            customerRepository.save(c);
            String qrUrl = twoFactorAuthService.generateQRUrl(c);
            model.addAttribute("qr", qrUrl);
            model.addAttribute("username", c.getUsername());
            return "qrcode";
        }
        
        customerRepository.save(c);
        model.addAttribute("success", true);
        return "register-success";
    }

    public static class CustomerForm {
        @NotBlank @Size(min=3,max=40) private String username;
        @NotBlank @Size(min=12,max=128) private String password; // Strength enforced centrally
        @NotBlank @Size(max=60) private String name;
        @NotBlank @Size(max=60) private String surname;
        @NotBlank @Size(max=120) private String address;
        @NotBlank @Pattern(regexp = "^\\+?[0-9\\- ]{7,20}$") private String phoneNumber;
        @NotBlank @Email @Size(max=120) private String email;
        private boolean using2FA = false;
        
        public String getUsername(){return username;} public void setUsername(String u){this.username=u;}
        public String getPassword(){return password;} public void setPassword(String p){this.password=p;}
        public String getName(){return name;} public void setName(String n){this.name=n;}
        public String getSurname(){return surname;} public void setSurname(String s){this.surname=s;}
        public String getAddress(){return address;} public void setAddress(String a){this.address=a;}
        public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String p){this.phoneNumber=p;}
        public String getEmail(){return email;} public void setEmail(String e){this.email=e;}
        public boolean isUsing2FA(){return using2FA;} public void setUsing2FA(boolean using2FA){this.using2FA=using2FA;}
    }
}
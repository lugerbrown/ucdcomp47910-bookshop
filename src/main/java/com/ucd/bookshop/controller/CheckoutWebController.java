package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Cart;
import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.repository.CustomerRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CheckoutWebController {
    private final CustomerRepository customerRepository;

    public CheckoutWebController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/checkout")
    public String showCheckout(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        Customer customer = customerRepository.findByUsername(userDetails.getUsername());
        if (customer == null || customer.getCart() == null || customer.getCart().getItems().isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("cart", customer.getCart());
        model.addAttribute("payment", new PaymentForm());
        return "checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(@AuthenticationPrincipal UserDetails userDetails, @ModelAttribute PaymentForm payment, Model model) {
        if (userDetails == null) return "redirect:/login";
        Customer customer = customerRepository.findByUsername(userDetails.getUsername());
        if (customer == null || customer.getCart() == null || customer.getCart().getItems().isEmpty()) {
            return "redirect:/cart";
        }
        // No need to store payment info or order in DB
        model.addAttribute("success", true);
        return "checkout-success";
    }

    public static class PaymentForm {
        private String creditCardNumber;
        public String getCreditCardNumber() { return creditCardNumber; }
        public void setCreditCardNumber(String creditCardNumber) { this.creditCardNumber = creditCardNumber; }
    }
} 
package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Book;
import com.ucd.bookshop.model.Cart;
import com.ucd.bookshop.model.CartItem;
import com.ucd.bookshop.model.Customer;
import com.ucd.bookshop.repository.BookRepository;
import com.ucd.bookshop.repository.CartItemRepository;
import com.ucd.bookshop.repository.CartRepository;
import com.ucd.bookshop.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@AllArgsConstructor
public class CartWebController {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;

    @GetMapping("/cart")
    public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        Customer customer = customerRepository.findByUsername(userDetails.getUsername());
        if (customer == null) return "redirect:/login";
        Cart cart = customer.getCart();
        if (cart == null) {
            cart = new Cart(customer);
            cartRepository.save(cart);
            customer.setCart(cart);
            customerRepository.save(customer);
        }
        model.addAttribute("cart", cart);
        model.addAttribute("items", cart.getItems());
        model.addAttribute("total", cart.getTotalPrice());
        return "cart";
    }

    @PostMapping("/cart/add/{bookId}")
    public String addToCart(@PathVariable Long bookId, @RequestParam(defaultValue = "1") int quantity, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        Customer customer = customerRepository.findByUsername(userDetails.getUsername());
        if (customer == null) return "redirect:/login";
        Cart cart = customer.getCart();
        if (cart == null) {
            cart = new Cart(customer);
            cartRepository.save(cart);
            customer.setCart(cart);
            customerRepository.save(customer);
        }
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) return "redirect:/books";
        CartItem item = new CartItem(cart, book, quantity);
        cartItemRepository.save(item);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return "redirect:/login";
        cartItemRepository.deleteById(itemId);
        return "redirect:/cart";
    }
} 
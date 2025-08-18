package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.CartItem;
import com.ucd.bookshop.repository.CartItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart-items")
public class CartItemController {
    private CartItemRepository cartItemRepository;

    public CartItemController(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @GetMapping("/{id}")
    public CartItem getCartItemById(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        CartItem item = cartItemRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (item.getCart() == null || item.getCart().getCustomer() == null || !item.getCart().getCustomer().getUsername().equals(principal.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return item;
    }
} 
package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.CartItem;
import com.ucd.bookshop.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/cart-items")
public class CartItemController {
    @Autowired
    private CartItemRepository cartItemRepository;

    @GetMapping
    public List<CartItem> getAllCartItems() {
        return cartItemRepository.findAll();
    }

    @GetMapping("/{id}")
    public CartItem getCartItemById(@PathVariable Long id) {
        return cartItemRepository.findById(id).orElse(null);
    }
} 
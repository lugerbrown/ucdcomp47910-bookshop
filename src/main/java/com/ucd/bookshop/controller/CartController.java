package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Cart;
import com.ucd.bookshop.model.CartItem;
import com.ucd.bookshop.model.Book;
import com.ucd.bookshop.repository.CartRepository;
import com.ucd.bookshop.repository.CartItemRepository;
import com.ucd.bookshop.repository.BookRepository;
import com.ucd.bookshop.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/carts")
public class CartController {
    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private BookRepository bookRepository;
    private CustomerRepository customerRepository;

    public CartController(CartRepository cartRepository, CartItemRepository cartItemRepository, BookRepository bookRepository, CustomerRepository customerRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/by-customer/{customerId}")
    public Cart getCartByCustomerId(@PathVariable Long customerId) {
        return cartRepository.findByCustomerId(customerId);
    }

    @GetMapping("/{cartId}/items")
    public List<CartItem> getCartItems(@PathVariable Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElse(null);
        return cart != null ? cart.getItems() : null;
    }

    @PostMapping("/{cartId}/add-item")
    public CartItem addItemToCart(@PathVariable Long cartId, @RequestBody AddItemRequest request) {
        Cart cart = cartRepository.findById(cartId).orElse(null);
        Book book = bookRepository.findById(request.bookId).orElse(null);
        if (cart == null || book == null) return null;
        CartItem item = new CartItem(cart, book, request.quantity);
        return cartItemRepository.save(item);
    }

    @DeleteMapping("/{cartId}/remove-item/{itemId}")
    public void removeItemFromCart(@PathVariable Long cartId, @PathVariable Long itemId) {
        cartItemRepository.deleteById(itemId);
    }

    @GetMapping("/{cartId}/total-price")
    public double getTotalPrice(@PathVariable Long cartId) {
        Cart cart = cartRepository.findById(cartId).orElse(null);
        return cart != null ? cart.getTotalPrice() : 0.0;
    }

    public static class AddItemRequest {
        public Long bookId;
        public int quantity;
    }
} 
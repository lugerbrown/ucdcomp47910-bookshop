package com.ucd.bookshop.controller;

import com.ucd.bookshop.model.Cart;
import com.ucd.bookshop.model.CartItem;
import com.ucd.bookshop.model.Book;
import com.ucd.bookshop.repository.CartRepository;
import com.ucd.bookshop.repository.CartItemRepository;
import com.ucd.bookshop.repository.BookRepository;
import com.ucd.bookshop.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
    public Cart getCartByCustomerId(@PathVariable Long customerId, @AuthenticationPrincipal UserDetails principal) {
        enforceCustomerOwnership(customerId, principal);
        return cartRepository.findByCustomerId(customerId);
    }

    @GetMapping("/{cartId}/items")
    public List<CartItem> getCartItems(@PathVariable Long cartId, @AuthenticationPrincipal UserDetails principal) {
        Cart cart = resolvedOwnedCart(cartId, principal);
        return cart.getItems();
    }

    @PostMapping("/{cartId}/add-item")
    public CartItem addItemToCart(@PathVariable Long cartId, @Valid @RequestBody AddItemRequest request, @AuthenticationPrincipal UserDetails principal) {
        Cart cart = resolvedOwnedCart(cartId, principal);
    Book book = bookRepository.findById(request.getBookId()).orElse(null);
        if (book == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
    CartItem item = new CartItem(cart, book, request.getQuantity());
        return cartItemRepository.save(item);
    }

    @DeleteMapping("/{cartId}/remove-item/{itemId}")
    public void removeItemFromCart(@PathVariable Long cartId, @PathVariable Long itemId, @AuthenticationPrincipal UserDetails principal) {
        Cart cart = resolvedOwnedCart(cartId, principal); // ensures cart ownership
        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // Ensure the item actually belongs to the owned cart to prevent cross-cart deletion (IDOR)
        if (item.getCart() == null || !item.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        cartItemRepository.deleteById(itemId);
    }

    @GetMapping("/{cartId}/total-price")
    public double getTotalPrice(@PathVariable Long cartId, @AuthenticationPrincipal UserDetails principal) {
        Cart cart = resolvedOwnedCart(cartId, principal);
        return cart.getTotalPrice();
    }

    private void enforceCustomerOwnership(Long customerId, UserDetails principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var customer = customerRepository.findByUsername(principal.getUsername());
        if (customer == null || !customer.getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Cart resolvedOwnedCart(Long cartId, UserDetails principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var owner = cart.getCustomer();
        if (owner == null || !owner.getUsername().equals(principal.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return cart;
    }

    public static class AddItemRequest {
        private Long bookId;
        @Min(value = 1, message = "Quantity must be >= 1")
        private int quantity;
        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
} 
package com.ucd.bookshop.controller;

import com.ucd.bookshop.config.SecurityAuditService;
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
    private SecurityAuditService auditService;

    public CartController(CartRepository cartRepository, CartItemRepository cartItemRepository, 
                         BookRepository bookRepository, CustomerRepository customerRepository,
                         SecurityAuditService auditService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.customerRepository = customerRepository;
        this.auditService = auditService;
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
            auditService.logValidationFailure("bookId", String.valueOf(request.getBookId()), 
                    "Book not found", "/carts/" + cartId + "/add-item");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
        
        CartItem item = new CartItem(cart, book, request.getQuantity());
        CartItem savedItem = cartItemRepository.save(item);
        
        // Log successful cart modification
        auditService.logAdminOperation("ADD_CART_ITEM", "cart/" + cartId, 
                "Added book " + book.getBookName() + " (quantity: " + request.getQuantity() + ") to cart");
        
        return savedItem;
    }

    @DeleteMapping("/{cartId}/remove-item/{itemId}")
    public void removeItemFromCart(@PathVariable Long cartId, @PathVariable Long itemId, @AuthenticationPrincipal UserDetails principal) {
        Cart cart = resolvedOwnedCart(cartId, principal); // ensures cart ownership
        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item == null) {
            auditService.logAuthorizationFailure("/carts/" + cartId + "/remove-item/" + itemId, "DELETE", "Cart item not found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        // Ensure the item actually belongs to the owned cart to prevent cross-cart deletion (IDOR)
        if (item.getCart() == null || !item.getCart().getId().equals(cart.getId())) {
            auditService.logAuthorizationFailure("/carts/" + cartId + "/remove-item/" + itemId, "DELETE", 
                    "IDOR attempt: User attempted to remove item from different cart");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
        cartItemRepository.deleteById(itemId);
        
        // Log successful cart item removal
        auditService.logAdminOperation("REMOVE_CART_ITEM", "cart/" + cartId, 
                "Removed item " + itemId + " from cart");
    }

    @GetMapping("/{cartId}/total-price")
    public double getTotalPrice(@PathVariable Long cartId, @AuthenticationPrincipal UserDetails principal) {
        Cart cart = resolvedOwnedCart(cartId, principal);
        return cart.getTotalPrice();
    }

    private void enforceCustomerOwnership(Long customerId, UserDetails principal) {
        if (principal == null) {
            auditService.logAuthorizationFailure("/carts/by-customer/" + customerId, "GET", "No authentication principal");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        var customer = customerRepository.findByUsername(principal.getUsername());
        if (customer == null || !customer.getId().equals(customerId)) {
            auditService.logAuthorizationFailure("/carts/by-customer/" + customerId, "GET", 
                    "User " + principal.getUsername() + " attempted to access cart for customer " + customerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Cart resolvedOwnedCart(Long cartId, UserDetails principal) {
        if (principal == null) {
            auditService.logAuthorizationFailure("/carts/" + cartId, "ACCESS", "No authentication principal");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        
        Cart cart = cartRepository.findById(cartId).orElse(null);
        if (cart == null) {
            auditService.logAuthorizationFailure("/carts/" + cartId, "ACCESS", "Cart not found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        var owner = cart.getCustomer();
        if (owner == null || !owner.getUsername().equals(principal.getUsername())) {
            auditService.logAuthorizationFailure("/carts/" + cartId, "ACCESS", 
                    "User " + principal.getUsername() + " attempted to access cart owned by " + 
                    (owner != null ? owner.getUsername() : "unknown"));
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
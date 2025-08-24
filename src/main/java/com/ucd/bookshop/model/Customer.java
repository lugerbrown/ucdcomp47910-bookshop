package com.ucd.bookshop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer extends User {
    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    private LocalDate dateOfBirth;
    @NotBlank
    private String address;
    @NotBlank
    private String phoneNumber;
    @Email
    @NotBlank
    private String email;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cart cart;

    public Customer() {
        super();
        setRole(Role.CUSTOMER);
    }
    public Customer(String username, String password, String name, String surname, LocalDate dateOfBirth, String address, String phoneNumber, String email) {
        super(username, password, Role.CUSTOMER);
        this.name = name;
        this.surname = surname;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
} 
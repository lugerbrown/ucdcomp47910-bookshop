package com.ucd.bookshop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends User {
    public Admin() {
        super();
        setRole(Role.ADMIN);
    }
    public Admin(String username, String password) {
        super(username, password, Role.ADMIN);
    }
} 
package com.auction.client.model;

import java.io.*;
import java.time.LocalDateTime;
public class User implements Serializable{
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String role;
    private double balance;
    private LocalDateTime createdAt;
    private boolean active;

    public User() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isBidder() { return "BIDDER".equals(role); }
    public boolean isSeller() { return "SELLER".equals(role); }
    public boolean isAdmin() { return "ADMIN".equals(role); }

}

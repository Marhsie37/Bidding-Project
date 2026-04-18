package com.auction.client.controller;

public class User {
    private String username;
    private String fullname;
    private String email;
    private String password;
    private String role;
    private String status;

    public User(String username, String fullname, String email, String password, String role) {
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = "ACTIVE";
    }


    public String getUsername() { return username; }
    public String getFullname() { return fullname; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPassword() { return password; }
    public String getEmail() { return email; }

}
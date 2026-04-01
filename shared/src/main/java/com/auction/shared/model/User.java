package com.auction.shared.model;


public abstract class User extends Entity {
    private String username;
    private String password;

    public User(String id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
}
package com.auction.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DataManager {
    public static ObservableList<Product> sharedProductList = FXCollections.observableArrayList();

    public static ObservableList<User> allUsers = FXCollections.observableArrayList();


    static {
        allUsers.add(new User("admin", "Hệ Thống", "admin@gmail.com", "123", "ADMIN"));
    }
}


package com.auction.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DataManager {
    public static ObservableList<Product> sharedProductList = FXCollections.observableArrayList();
}
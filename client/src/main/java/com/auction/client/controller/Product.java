package com.auction.client.controller;

public class Product {
    private String name;
    private String price;
    private String imageUrl;
    private long endTime;

    public Product(String name, String price, String imageUrl, int durationInSeconds) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.endTime = System.currentTimeMillis() + (durationInSeconds * 1000L);
    }


    public void setPrice(String price) {
        this.price = price;
    }


    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }

    public int getRemainingSeconds() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return (remaining > 0) ? (int) remaining : 0;
    }
}
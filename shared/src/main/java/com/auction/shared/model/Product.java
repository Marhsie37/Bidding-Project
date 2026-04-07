package com.auction.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private int sellerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // PENDING, RUNNING, FINISHED
    private LocalDateTime createdAt;

    public Product() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    public Product(int id, String name, double startingPrice, int sellerId) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.sellerId = sellerId;
        this.createdAt = LocalDateTime.now();
    }

    public abstract String getCategoryName();

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class Electronics extends Product {
    private String brand;
    private String warrantyPeriod;

    @Override
    public String getCategoryName() {
        return "ELECTRONICS";
    }

    // Getter/Setter riêng cho đặc tính đồ điện tử
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}
class Art extends Product {
    private String artist;
    private int yearCreated;

    @Override
    public String getCategoryName() {
        return "ART";
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
}
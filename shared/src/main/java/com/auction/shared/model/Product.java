package com.auction.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private int sellerId;
    private String sellerName;
    private String category;
    private String imageUrl;
    private int durationHours;
    private int durationSeconds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private int winnerId;
    private String winnerName;
    private LocalDateTime createdAt;

    public Product() {}

    // ========== GETTERS ==========
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public int getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public int getDurationHours() { return durationHours; }
    public int getDurationSeconds() { return durationSeconds; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public int getWinnerId() { return winnerId; }
    public String getWinnerName() { return winnerName; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ========== SETTERS ==========
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setCategory(String category) { this.category = category; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setStatus(String status) { this.status = status; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ========== UTILITY ==========
    public int getRemainingSeconds() {
        if (endTime == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) return 0;
        return (int) ChronoUnit.SECONDS.between(now, endTime);
    }
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }


}
package com.auction.shared.model;

import java.time.LocalDateTime;

public class Auction extends Entity {
    private String itemId;
    private double currentPrice;
    private String highestBidderId;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public enum AuctionStatus {
        OPEN, RUNNING, FINISHED, CANCELED
    }

    public Auction(String id, String itemId, double startingPrice, LocalDateTime endTime) {
        super(id);
        this.itemId = itemId;
        this.currentPrice = startingPrice;
        this.status = AuctionStatus.OPEN;
    }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
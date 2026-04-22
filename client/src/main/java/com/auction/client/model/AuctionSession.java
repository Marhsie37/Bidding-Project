package com.auction.client.model;

import java.io.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionSession implements Serializable{
    private static final long serialVersionUID = 1L;

    private int productId;
    private String productName;
    private double currentPrice;
    private int currentWinnerId;
    private String currentWinnerName;
    private LocalDateTime endTime;
    private String status;
    private ConcurrentHashMap<String, Double> autoBids;
    private LocalDateTime scheduledEndTime;
    private int extensionCount;

    public AuctionSession() {
        this.autoBids = new ConcurrentHashMap<>();
    }

    public AuctionSession(int productId, String productName, double startingPrice, LocalDateTime endTime) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.currentPrice = startingPrice;
        this.endTime = endTime;
        this.status = "OPEN";
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public int getCurrentWinnerId() { return currentWinnerId; }
    public void setCurrentWinnerId(int currentWinnerId) { this.currentWinnerId = currentWinnerId; }

    public String getCurrentWinnerName() { return currentWinnerName; }
    public void setCurrentWinnerName(String currentWinnerName) { this.currentWinnerName = currentWinnerName; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ConcurrentHashMap<String, Double> getAutoBids() { return autoBids; }
    public void setAutoBids(ConcurrentHashMap<String, Double> autoBids) { this.autoBids = autoBids; }

    public LocalDateTime getScheduledEndTime() {
        return scheduledEndTime;
    }

    public void setScheduledEndTime(LocalDateTime scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public int getExtensionCount() {
        return extensionCount;
    }

    public void setExtensionCount(int extensionCount) {
        this.extensionCount = extensionCount;
    }
    public int getId() {
        return productId;
    }

}

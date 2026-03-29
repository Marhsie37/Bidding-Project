package com.auction.client.model;
public class Product{
    private int id;
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private int currentWinnerId;
    private long endTime;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public int getCurrentWinnerId() { return currentWinnerId; }
    public void setCurrentWinnerId(int currentWinnerId) { this.currentWinnerId = currentWinnerId; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public String getFormattedEndTime(){
        long remaining = (endTime - System.currentTimeMillis())/1000;
        if (remaining <= 0) return "Auction ended";
        long minutes = remaining / 60;
        long seconds = remaining % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
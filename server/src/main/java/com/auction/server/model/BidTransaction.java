package com.auction.server.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int auctionId;
    private int bidderId;
    private String bidderName;
    private double bidAmount;
    private LocalDateTime bidTime;
    private boolean isAutoBid;

    public BidTransaction() {}

    public BidTransaction(int auctionId, int bidderId, String bidderName, double bidAmount, boolean isAutoBid) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
        this.isAutoBid = isAutoBid;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getBidderId() { return bidderId; }
    public void setBidderId(int bidderId) { this.bidderId = bidderId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public boolean isAutoBid() { return isAutoBid; }
    public void setAutoBid(boolean isAutoBid) { this.isAutoBid = isAutoBid; }
}
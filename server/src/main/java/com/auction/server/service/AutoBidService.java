package com.auction.server.service;

public class AutoBidService {
    private static AutoBidService instance;
    private AutoBidService() {}
    public static synchronized AutoBidService getInstance() {
        if (instance == null) {
            instance = new AutoBidService();
        }
        return instance;
    }
    public void start() { System.out.println("AutoBidService start"); }
    public void stop() { System.out.println("AutoBidService stop"); }
}
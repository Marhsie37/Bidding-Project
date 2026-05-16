package com.auction.server.service;

import com.auction.shared.model.AuctionSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoBidService {

    private static AutoBidService instance;
    private ScheduledExecutorService scheduler;
    private AuctionService auctionService;

    // Lưu auto bid: productId -> (username -> maxBid)
    private Map<Integer, Map<String, Double>> autoBidConfigs = new ConcurrentHashMap<>();

    private AutoBidService() {}

    public static synchronized AutoBidService getInstance() {
        if (instance == null) {
            instance = new AutoBidService();
        }
        return instance;
    }

    public void start() {
        auctionService = AuctionService.getInstance();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::processAllAutoBids, 0, 2, TimeUnit.SECONDS);
        System.out.println("AutoBidService: Hệ thống tự động đặt giá đã khởi chạy!");
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("AutoBidService: Đã tắt.");
        }
    }

    // Đăng ký auto bid cho user
    public void registerAutoBid(int productId, String username, double maxBid) {
        autoBidConfigs.computeIfAbsent(productId, k -> new ConcurrentHashMap<>())
                .put(username, maxBid);
        System.out.println("✅ AutoBid đăng ký: user=" + username + ", product=" + productId + ", max=" + maxBid);
    }

    // Hủy auto bid của user
    public void unregisterAutoBid(int productId, String username) {
        Map<String, Double> bids = autoBidConfigs.get(productId);
        if (bids != null) {
            bids.remove(username);
            if (bids.isEmpty()) {
                autoBidConfigs.remove(productId);
            }
        }
    }

    // Xử lý tất cả auto bid
    private void processAllAutoBids() {
        if (auctionService == null) return;

        for (Map.Entry<Integer, Map<String, Double>> entry : autoBidConfigs.entrySet()) {
            int productId = entry.getKey();
            Map<String, Double> bidders = entry.getValue();

            // Lấy thông tin session hiện tại
            AuctionSession session = auctionService.getSession(productId);
            if (session == null) continue;
            if (!"ACTIVE".equals(session.getStatus())) continue;

            double currentPrice = session.getCurrentPrice();
            String currentWinner = session.getCurrentWinnerName();

            // Tìm người có maxBid cao nhất
            String topBidder = null;
            double topMaxBid = 0;

            for (Map.Entry<String, Double> bidder : bidders.entrySet()) {
                if (bidder.getValue() > topMaxBid) {
                    topMaxBid = bidder.getValue();
                    topBidder = bidder.getKey();
                }
            }

            // Nếu người đang dẫn đầu không phải là người có maxBid cao nhất
            if (topBidder != null && !topBidder.equals(currentWinner)) {
                // Tính giá tiếp theo
                double nextBid = currentPrice + 1000; // Bước nhảy mặc định

                if (nextBid <= topMaxBid && nextBid > currentPrice) {
                    System.out.println("[AUTO-BID] " + topBidder + " tự động đặt giá " + nextBid + " cho sản phẩm " + productId);
                    auctionService.placeBid(productId, topBidder, nextBid);
                }
            }
        }
    }

    // Lấy danh sách auto bid của sản phẩm
    public Map<String, Double> getAutoBids(int productId) {
        return autoBidConfigs.getOrDefault(productId, new ConcurrentHashMap<>());
    }
}
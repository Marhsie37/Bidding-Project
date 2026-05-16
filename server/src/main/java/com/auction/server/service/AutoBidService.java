package com.auction.server.service;

import com.auction.shared.model.AuctionSession;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoBidService {

    // SINGLETON
    private static AutoBidService instance;

    // Bộ chạy ngầm đa luồng
    private ScheduledExecutorService scheduler;

    private AutoBidService() {}

    public static synchronized AutoBidService getInstance() {
        if (instance == null) {
            instance = new AutoBidService();
        }
        return instance;
    }

    public void start() {
        scheduler = Executors.newScheduledThreadPool(2);

        scheduler.scheduleAtFixedRate(this::processAllAutoBids, 0, 3, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(this::checkAndCloseExpiredAuctions, 0, 1, TimeUnit.SECONDS);

        System.out.println("AutoBidService: Hệ thống tự động đặt giá & Quản lý vòng đời đấu giá đã khởi chạy!");
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("AutoBidService: Đã tắt.");
        }
    }

    private void checkAndCloseExpiredAuctions() {
        AuctionService.getInstance().checkAndEndAuctions();
    }

    // Lõi thuật toán Auto-Bid
    @SuppressWarnings("unchecked")
    private void processAllAutoBids() {
        AuctionService auctionService = AuctionService.getInstance();
        Map<String, Object> activeData = auctionService.getActiveProducts();

        if (!activeData.containsKey("products")) return;
        List<AuctionSession> activeSessions = (List<AuctionSession>) activeData.get("products");

        for (AuctionSession session : activeSessions) {
            Map<String, Double> autoBids = session.getAutoBids();
            if (autoBids == null || autoBids.isEmpty()) continue;

            PriorityQueue<AutoBidTask> queue = new PriorityQueue<>((a, b) -> Double.compare(b.maxBid, a.maxBid));

            for (Map.Entry<String, Double> entry : autoBids.entrySet()) {
                queue.add(new AutoBidTask(entry.getKey(), entry.getValue()));
            }

            AutoBidTask topBidder = queue.poll();

            if (topBidder != null && !topBidder.username.equals(session.getCurrentWinnerName())) {

                double nextBid = session.getCurrentPrice() + 10.0;

                if (topBidder.maxBid >= nextBid) {
                    System.out.println("[AUTO-BID] Tự động nâng giá cho user [" + topBidder.username + "] lên mức " + nextBid);
                    auctionService.placeBid(session.getProductId(), topBidder.username, nextBid);
                }
            }
        }
    }

    private static class AutoBidTask {
        String username;
        double maxBid;

        AutoBidTask(String username, double maxBid) {
            this.username = username;
            this.maxBid = maxBid;
        }
    }
}
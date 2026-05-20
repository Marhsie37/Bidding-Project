package com.auction.server.service;

import com.auction.shared.model.AuctionSession;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoBidService {

    // SINGLETON
    private static AutoBidService instance;

    // Bộ chạy ngầm đa luồng
    private ScheduledExecutorService scheduler;
    private AuctionService auctionService;
    private static final Logger logger = LoggerFactory.getLogger(AutoBidService.class);
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
        logger.info("AutoBidService: Hệ thống tự động đặt giá đã khởi chạy!");
        scheduler = Executors.newScheduledThreadPool(2);

        scheduler.scheduleAtFixedRate(this::processAllAutoBids, 0, 3, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(this::checkAndCloseExpiredAuctions, 0, 1, TimeUnit.SECONDS);

        System.out.println("AutoBidService: Hệ thống tự động đặt giá & Quản lý vòng đời đấu giá đã khởi chạy!");
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            logger.info("AutoBidService: Đã tắt.");
        }
    }

    private void checkAndCloseExpiredAuctions() {
        AuctionService.getInstance().checkAndEndAuctions();
    // Đăng ký auto bid cho user
    public void registerAutoBid(int productId, String username, double maxBid) {
        autoBidConfigs.computeIfAbsent(productId, k -> new ConcurrentHashMap<>())
                .put(username, maxBid);
        logger.info("✅ AutoBid đăng ký: user=" + username + ", product=" + productId + ", max=" + maxBid);
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
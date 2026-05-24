package com.auction.server.service;

import com.auction.shared.model.AuctionSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoBidService {

    public static class AutoBidConfig {
        private final double maxBid;
        private final double increment;

        public AutoBidConfig(double maxBid, double increment) {
            this.maxBid = maxBid;
            this.increment = increment;
        }

        public double getMaxBid() {
            return maxBid;
        }

        public double getIncrement() {
            return increment;
        }
    }

    private static AutoBidService instance;
    private ScheduledExecutorService scheduler;
    private AuctionService auctionService;
    private static final Logger logger = LoggerFactory.getLogger(AutoBidService.class);
    private Map<Integer, Map<String, AutoBidConfig>> autoBidConfigs = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean processing = new java.util.concurrent.atomic.AtomicBoolean(false);

    private AutoBidService() {
    }

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
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            logger.info("AutoBidService: Đã tắt.");
        }
    }

    public void registerAutoBid(int productId, String username, double maxBid, double increment) {
        autoBidConfigs.computeIfAbsent(productId, k -> new ConcurrentHashMap<>())
                .put(username, new AutoBidConfig(maxBid, increment));
        logger.info("✅ AutoBid đăng ký: user=" + username + ", product=" + productId + ", max=" + maxBid + ", inc=" + increment);
    }

    public void registerAutoBid(int productId, String username, double maxBid) {
        registerAutoBid(productId, username, maxBid, 5000.0);
    }

    public void unregisterAutoBid(int productId, String username) {
        Map<String, AutoBidConfig> bids = autoBidConfigs.get(productId);
        if (bids != null) {
            bids.remove(username);
            if (bids.isEmpty()) {
                autoBidConfigs.remove(productId);
            }
        }
    }

    public void removeProductAutoBids(int productId) {
        autoBidConfigs.remove(productId);
        logger.info("🧹 Đã dọn dẹp Auto-bid cho sản phẩm: " + productId);
    }

    public void processAllAutoBids() {
        if (auctionService == null) return;
        if (!processing.compareAndSet(false, true)) {
            return;
        }

        try {
            boolean bidPlaced;
            do {
                bidPlaced = false;
                for (Map.Entry<Integer, Map<String, AutoBidConfig>> entry : autoBidConfigs.entrySet()) {
                    int productId = entry.getKey();
                    Map<String, AutoBidConfig> bidders = entry.getValue();

                    Map<String, Object> details = auctionService.getAuctionDetails(productId);
                    AuctionSession session = (AuctionSession) details.get("session");
                    if (session == null) continue;
                    if (!"ACTIVE".equals(session.getStatus())) continue;

                    double currentPrice = session.getCurrentPrice();
                    String currentWinner = session.getCurrentWinnerName();

                    String eligibleBidder = null;
                    AutoBidConfig eligibleConfig = null;
                    double highestMaxBid = 0;

                    for (Map.Entry<String, AutoBidConfig> bidder : bidders.entrySet()) {
                        String username = bidder.getKey();
                        AutoBidConfig config = bidder.getValue();

                        if (username.equals(currentWinner)) {
                            continue;
                        }

                        double step = Math.max(config.getIncrement(), 5000.0);
                        double nextBid = currentPrice + step;

                        if (nextBid <= config.getMaxBid()) {
                            if (config.getMaxBid() > highestMaxBid) {
                                highestMaxBid = config.getMaxBid();
                                eligibleBidder = username;
                                eligibleConfig = config;
                            }
                        }
                    }

                    if (eligibleBidder != null) {
                        double step = Math.max(eligibleConfig.getIncrement(), 5000.0);
                        double nextBid = currentPrice + step;
                        logger.info("[AUTO-BID] " + eligibleBidder + " tự động đặt giá " + nextBid + " cho sản phẩm " + productId);
                        auctionService.placeBid(productId, eligibleBidder, nextBid);
                        bidPlaced = true;
                    }
                }
            } while (bidPlaced);
        } finally {
            processing.set(false);
        }
    }

    public Map<String, Double> getAutoBids(int productId) {
        Map<String, AutoBidConfig> configs = autoBidConfigs.get(productId);
        if (configs == null) {
            return new ConcurrentHashMap<>();
        }
        Map<String, Double> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, AutoBidConfig> entry : configs.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getMaxBid());
        }
        return result;
    }
}
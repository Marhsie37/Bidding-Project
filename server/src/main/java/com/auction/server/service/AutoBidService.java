package com.auction.server.service;

import com.auction.shared.model.AuctionSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoBidService {

    public static class AutoBidConfig {
        private final double maxBid;
        private final double increment;

        public AutoBidConfig(double maxBid, double increment) {
            this.maxBid = maxBid;
            this.increment = increment;
        }

        public double getMaxBid() { return maxBid; }
        public double getIncrement() { return increment; }
    }

    private static AutoBidService instance;
    private ScheduledExecutorService scheduler;
    private AuctionService auctionService;
    private static final Logger logger = LoggerFactory.getLogger(AutoBidService.class);
    // Lưu auto bid: productId -> (username -> AutoBidConfig)
    private Map<Integer, Map<String, AutoBidConfig>> autoBidConfigs = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean processing = new java.util.concurrent.atomic.AtomicBoolean(false);

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
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            logger.info("AutoBidService: Đã tắt.");
        }
    }

    // Đăng ký auto bid cho user với increment
    public void registerAutoBid(int productId, String username, double maxBid, double increment) {
        autoBidConfigs.computeIfAbsent(productId, k -> new ConcurrentHashMap<>())
                .put(username, new AutoBidConfig(maxBid, increment));
        logger.info("✅ AutoBid đăng ký: user=" + username + ", product=" + productId + ", max=" + maxBid + ", inc=" + increment);
    }

    // Đăng ký auto bid cho user (backward compatibility)
    public void registerAutoBid(int productId, String username, double maxBid) {
        registerAutoBid(productId, username, maxBid, 5000.0);
    }

    // Hủy auto bid của user
    public void unregisterAutoBid(int productId, String username) {
        Map<String, AutoBidConfig> bids = autoBidConfigs.get(productId);
        if (bids != null) {
            bids.remove(username);
            if (bids.isEmpty()) {
                autoBidConfigs.remove(productId);
            }
        }
    }

    // Xóa toàn bộ auto bid của một sản phẩm khi phiên đấu giá kết thúc
    public void removeProductAutoBids(int productId) {
        autoBidConfigs.remove(productId);
        logger.info("🧹 Đã dọn dẹp Auto-bid cho sản phẩm: " + productId);
    }

    // Xử lý tất cả auto bid
    public void processAllAutoBids() {
        if (auctionService == null) return;
        if (!processing.compareAndSet(false, true)) {
            // Đang xử lý, tránh gọi đệ quy lặp lại
            return;
        }

        try {
            boolean bidPlaced;
            do {
                bidPlaced = false;
                for (Map.Entry<Integer, Map<String, AutoBidConfig>> entry : autoBidConfigs.entrySet()) {
                    int productId = entry.getKey();
                    Map<String, AutoBidConfig> bidders = entry.getValue();

                    // Lấy thông tin session hiện tại
                    Map<String, Object> details = auctionService.getAuctionDetails(productId);
                    AuctionSession session = (AuctionSession) details.get("session");
                    if (session == null) continue;
                    if (!"ACTIVE".equals(session.getStatus())) continue;

                    double currentPrice = session.getCurrentPrice();
                    String currentWinner = session.getCurrentWinnerName();

                    // Tìm người có maxBid cao nhất đang không phải là currentWinner và đủ điều kiện đặt giá tiếp theo
                    String eligibleBidder = null;
                    AutoBidConfig eligibleConfig = null;
                    double highestMaxBid = 0;

                    for (Map.Entry<String, AutoBidConfig> bidder : bidders.entrySet()) {
                        String username = bidder.getKey();
                        AutoBidConfig config = bidder.getValue();

                        // Người đang dẫn đầu không cần tự đấu với chính mình
                        if (username.equals(currentWinner)) {
                            continue;
                        }

                        // Tính giá đặt tiếp theo dựa trên bước giá của người đặt
                        double step = Math.max(config.getIncrement(), 5000.0);
                        double nextBid = currentPrice + step;

                        // Đảm bảo giá đặt tiếp theo không vượt quá mức tối đa (maxBid) của người đó
                        if (nextBid <= config.getMaxBid()) {
                            // Chọn người có maxBid cao nhất để đặt giá
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

    // Lấy danh sách auto bid của sản phẩm (backward compatibility)
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
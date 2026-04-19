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
        scheduler = Executors.newScheduledThreadPool(1);
        // Cứ 3 giây luồng này sẽ tự động quét một lần
        scheduler.scheduleAtFixedRate(this::processAllAutoBids, 0, 3, TimeUnit.SECONDS);
        System.out.println("AutoBidService: Hệ thống luồng tự động đặt giá đã khởi chạy!");
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("AutoBidService: Đã tắt.");
        }
    }

    // Lõi thuật toán Auto-Bid
    @SuppressWarnings("unchecked")
    private void processAllAutoBids() {
        // Mượn danh sách các phiên đấu giá đang mở từ AuctionService
        AuctionService auctionService = AuctionService.getInstance();
        Map<String, Object> activeData = auctionService.getActiveProducts();

        if (!activeData.containsKey("products")) return;
        List<AuctionSession> activeSessions = (List<AuctionSession>) activeData.get("products");

        for (AuctionSession session : activeSessions) {
            Map<String, Double> autoBids = session.getAutoBids();
            if (autoBids == null || autoBids.isEmpty()) continue;

            // Dùng PriorityQueue để xếp hạng ưu tiên (Ai cài MaxBid cao nhất thì lên đầu)
            PriorityQueue<AutoBidTask> queue = new PriorityQueue<>((a, b) -> Double.compare(b.maxBid, a.maxBid));

            for (Map.Entry<String, Double> entry : autoBids.entrySet()) {
                queue.add(new AutoBidTask(entry.getKey(), entry.getValue()));
            }

            AutoBidTask topBidder = queue.poll();

            if (topBidder != null && !topBidder.username.equals(session.getCurrentWinnerName())) {

                // Mặc định mỗi lần Auto-bid sẽ cộng thêm 10.0 vào giá hiện tại
                double nextBid = session.getCurrentPrice() + 10.0;

                // Kích hoạt đặt giá nếu ví tiền (maxBid) của họ vẫn tiếp tục được
                if (topBidder.maxBid >= nextBid) {
                    System.out.println("[AUTO-BID] Tự động nâng giá cho user [" + topBidder.username + "] lên mức " + nextBid);
                    // Gọi ngược lại hàm placeBid của AuctionService để chốt giá chuẩn quy trình
                    auctionService.placeBid(session.getProductId(), topBidder.username, nextBid);
                }
            }
        }
    }

    // Lớp phụ trợ bọc dữ liệu để ném vào Priority Queue
    private static class AutoBidTask {
        String username;
        double maxBid;

        AutoBidTask(String username, double maxBid) {
            this.username = username;
            this.maxBid = maxBid;
        }
    }
}

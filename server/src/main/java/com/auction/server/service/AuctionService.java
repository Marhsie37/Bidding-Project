package com.auction.server.service;

import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class AuctionService {

    // --- SINGLETON  ---
    private static AuctionService instance;

    private AuctionService() {}

    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }
    // ---------------------------

    private Map<Integer, AuctionSession> sessions = new ConcurrentHashMap<>();

    public void addSession(AuctionSession session) {
        sessions.put(session.getProductId(), session);
    }

    public BidTransaction placeBid(int auctionId, int bidderId, String bidderName, double bidAmount, boolean isAutoBid) {
        AuctionSession session = sessions.get(auctionId);

        if (session == null) {
            System.out.println("Lỗi: Phiên đấu giá không tồn tại!");
            return null;
        }

        if (!"ACTIVE".equals(session.getStatus()) || LocalDateTime.now().isAfter(session.getEndTime())) {
            System.out.println("Lỗi: Phiên đấu giá đã đóng cửa hoặc hết hạn!");
            session.setStatus("FINISHED");
            return null;
        }

        if (bidAmount <= session.getCurrentPrice()) {
            System.out.println("Lỗi: Giá đặt (" + bidAmount + ") phải cao hơn giá hiện hành (" + session.getCurrentPrice() + ")!");
            return null;
        }

        session.setCurrentPrice(bidAmount);
        session.setCurrentWinnerId(bidderId);
        session.setCurrentWinnerName(bidderName);

        BidTransaction newBid = new BidTransaction(auctionId, bidderId, bidderName, bidAmount, isAutoBid);

        System.out.println("Thành công: [" + bidderName + "] đang dẫn đầu với giá " + bidAmount);
        return newBid;
    }

    public void endAuction(int auctionId) {
        AuctionSession session = sessions.get(auctionId);
        if (session != null && !"FINISHED".equals(session.getStatus())) {
            session.setStatus("FINISHED");
            session.setEndTime(LocalDateTime.now());

            System.out.println("--- PHIÊN ĐẤU GIÁ [" + session.getProductName() + "] ĐÃ KẾT THÚC ---");
            if (session.getCurrentWinnerId() != 0) {
                System.out.println("Người thắng cuộc: [" + session.getCurrentWinnerName() + "] - Giá chốt: " + session.getCurrentPrice());
            } else {
                System.out.println("Phiên đấu giá ế ẩm, không có ai trả giá.");
            }
        }
    }

    // --- HÀM MỚI BỔ SUNG CHO SERVER: TỰ ĐỘNG QUÉT VÀ CHỐT SỔ ---
    public void checkAndEndAuctions() {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Integer, AuctionSession> entry : sessions.entrySet()) {
            AuctionSession session = entry.getValue();
            // Nếu phiên đang mở mà thời gian hiện tại đã vượt qua thời gian kết thúc
            if ("ACTIVE".equals(session.getStatus()) && now.isAfter(session.getEndTime())) {
                endAuction(entry.getKey());
            }
        }
    }
}
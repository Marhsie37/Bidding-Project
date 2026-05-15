package com.auction.server.dao;

import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionDAOTest {
    private AuctionDAO auctionDAO;
    private int testProductId = 1;
    private int testBidderId = 2;

    @BeforeAll
    void setup() {
        DatabaseConnection.getInstance();
        auctionDAO = new AuctionDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Test lấy thông tin phiên đấu giá")
    void testGetAuctionSession() {
        AuctionSession session = auctionDAO.getAuctionSession(testProductId);

        assertNotNull(session, "Phiên đấu giá không được null");
        System.out.println("Sản phẩm đang đấu giá: " + session.getProductName());
    }

    @Test
    @Order(2)
    @DisplayName("Test lưu lượt đặt giá mới (Bid)")
    void testSaveBid() {
        BidTransaction bid = new BidTransaction();
        bid.setAuctionId(testProductId);
        bid.setBidderId(testBidderId);
        bid.setBidAmount(10000.0);
        bid.setAutoBid(false);

        boolean result = auctionDAO.saveBid(bid);

        assertTrue(result, "Lưu lượt đặt giá phải thành công");
        assertTrue(bid.getId() > 0, "ID của bid phải được tự động tạo (Generated Keys)");
    }

    @Test
    @Order(3)
    @DisplayName("Test lấy giá cao nhất và người đặt cao nhất")
    void testHighestBidInfo() {
        double highestPrice = auctionDAO.getHighestBid(testProductId);
        int bidderId = auctionDAO.getHighestBidder(testProductId);

        assertTrue(highestPrice >= 10000.0, "Giá cao nhất phải lớn hơn hoặc bằng mức vừa đặt");
        assertEquals(testBidderId, bidderId, "Người đặt cao nhất phải là testBidderId");
    }

    @Test
    @Order(4)
    @DisplayName("Test lấy lịch sử đặt giá")
    void testGetBidHistory() {
        List<BidTransaction> history = auctionDAO.getBidHistory(testProductId, 10);

        assertNotNull(history);
        assertFalse(history.isEmpty(), "Lịch sử đặt giá không được trống");
        System.out.println("Số lượt đặt giá tìm thấy: " + history.size());
    }

    @Test
    @Order(5)
    @DisplayName("Test kết thúc phiên đấu giá")
    void testEndAuction() {
        boolean result = auctionDAO.endAuction(testProductId, testBidderId, 15000.0);

        assertTrue(result, "Cập nhật kết thúc đấu giá phải thành công");

        AuctionSession session = auctionDAO.getAuctionSession(testProductId);
        assertEquals("ENDED", session.getStatus(), "Trạng thái phải chuyển sang ENDED");
    }
}
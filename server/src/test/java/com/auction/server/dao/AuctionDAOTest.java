package com.auction.server.dao;

import com.auction.shared.model.AuctionSession;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Product;
import com.auction.shared.model.User;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionDAOTest {
    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private ProductDAO productDAO;
    private static final Logger logger = LoggerFactory.getLogger(AuctionDAOTest.class);
    private int testProductId;
    private int testBidderId;

    @BeforeAll
    void setup() {
        DatabaseConnection.getInstance();
        auctionDAO = new AuctionDAO();
        userDAO = new UserDAO();
        productDAO = new ProductDAO();

        String uniqueUser = "bidder_" + System.currentTimeMillis();
        userDAO.createUser(uniqueUser, "pass", uniqueUser + "@test.com", "Test Bidder", "BIDDER");
        testBidderId = userDAO.findByUsername(uniqueUser).getId();

        Product p = new Product();
        p.setName("Laptop Gaming Test");
        p.setStartingPrice(10000.0);
        p.setSellerId(1);
        p.setCategory("Electronics");
        p.setDurationHours(1);
        p.setEndTime(LocalDateTime.now().plusHours(1));

        productDAO.createProduct(p);
        testProductId = p.getId();

        logger.info("--- SETUP HOÀN TẤT: UserID=" + testBidderId + ", ProductID=" + testProductId + " ---");
    }

    @Test
    @Order(1)
    @DisplayName("Test lấy thông tin phiên đấu giá")
    void testGetAuctionSession() {
        AuctionSession session = auctionDAO.getAuctionSession(testProductId);
        assertNotNull(session, "Phiên đấu giá phải tồn tại cho sản phẩm ID: " + testProductId);
    }

    @Test
    @Order(2)
    @DisplayName("Test lưu lượt đặt giá mới (Bid)")
    void testSaveBid() {
        BidTransaction bid = new BidTransaction();
        bid.setAuctionId(testProductId);
        bid.setBidderId(testBidderId);
        bid.setBidAmount(12000.0); // Cao hơn giá khởi điểm
        bid.setAutoBid(false);

        boolean result = auctionDAO.saveBid(bid);

        assertTrue(result, "Lưu lượt đặt giá phải thành công khi User và Product đã tồn tại");
        assertTrue(bid.getId() > 0, "ID của bid phải được trả về");
    }

    @Test
    @Order(3)
    @DisplayName("Test lấy giá cao nhất và người đặt cao nhất")
    void testHighestBidInfo() {
        double highestPrice = auctionDAO.getHighestBid(testProductId);
        int bidderId = auctionDAO.getHighestBidder(testProductId);

        assertEquals(12000.0, highestPrice, "Giá cao nhất phải khớp với mức vừa đặt");
        assertEquals(testBidderId, bidderId, "Người đặt cao nhất phải là testBidderId");
    }

    @Test
    @Order(4)
    @DisplayName("Test lấy lịch sử đặt giá")
    void testGetBidHistory() {
        List<BidTransaction> history = auctionDAO.getBidHistory(testProductId, 10);
        assertNotNull(history);
        assertFalse(history.isEmpty(), "Lịch sử đặt giá không được trống sau khi đã đặt thành công");
    }

    @Test
    @Order(5)
    @DisplayName("Test kết thúc phiên đấu giá")
    void testEndAuction() {
        boolean result = auctionDAO.endAuction(testProductId, testBidderId, 12000.0);
        assertTrue(result);

        AuctionSession session = auctionDAO.getAuctionSession(testProductId);
        assertEquals("ENDED", session.getStatus(), "Trạng thái phải chuyển sang ENDED");
    }
}